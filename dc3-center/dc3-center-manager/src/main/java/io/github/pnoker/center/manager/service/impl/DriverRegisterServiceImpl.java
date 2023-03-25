/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.CodeQuery;
import io.github.pnoker.api.center.auth.RTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.dto.DriverMetadataDTO;
import io.github.pnoker.common.dto.DriverRegisterDTO;
import io.github.pnoker.common.entity.driver.DriverMetadata;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Driver;
import io.github.pnoker.common.model.DriverAttribute;
import io.github.pnoker.common.model.PointAttribute;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 驱动注册相关接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverRegisterServiceImpl implements DriverRegisterService {

    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;

    @Resource
    private BatchService batchService;

    @Resource
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverInfoService driverInfoService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointInfoService pointInfoService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void register(DriverRegisterDTO entityDTO) {
        if (ObjectUtil.isNull(entityDTO) || ObjectUtil.isNull(entityDTO.getDriver())) {
            return;
        }

        DriverMetadataDTO driverConfiguration = new DriverMetadataDTO(
                MetadataTypeEnum.METADATA,
                MetadataCommandTypeEnum.SYNC,
                null
        );

        try {
            Driver driver = registerDriver(entityDTO);
            registerDriverAttribute(entityDTO, driver);
            registerPointAttribute(entityDTO, driver);
            DriverMetadata driverMetadata = batchService.batchDriverMetadata(entityDTO.getDriver().getServiceName());
            driverConfiguration.setContent(JsonUtil.toJsonString(driverMetadata));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_METADATA,
                RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + entityDTO.getDriver().getServiceName(),
                driverConfiguration
        );
    }

    /**
     * 注册驱动
     *
     * @param driverRegisterDTO DriverRegisterDTO
     */
    private Driver registerDriver(DriverRegisterDTO driverRegisterDTO) {
        // check tenant
        RTenantDTO rTenantDTO = tenantApiBlockingStub.selectByCode(CodeQuery.newBuilder().setCode(driverRegisterDTO.getTenant()).build());
        if (!rTenantDTO.getResult().getOk()) {
            throw new ServiceException("Invalid {}, {}", driverRegisterDTO.getTenant(), rTenantDTO.getResult().getMessage());
        }

        // register driver
        Driver driver = driverRegisterDTO.getDriver();
        driver.setTenantId(rTenantDTO.getData().getBase().getId());
        log.info("Register driver {}", driver);
        try {
            Driver byServiceName = driverService.selectByServiceName(driver.getServiceName());
            log.debug("Driver already registered, updating {} ", driver);
            driver.setId(byServiceName.getId());
            driver = driverService.update(driver);
        } catch (NotFoundException notFoundException1) {
            log.debug("Driver does not registered, adding {} ", driver);
            driver = driverService.add(driver);
        }
        return driver;
    }

    /**
     * 注册驱动属性
     *
     * @param driverRegisterDTO DriverRegisterDTO
     * @param driver            Driver
     */
    private void registerDriverAttribute(DriverRegisterDTO driverRegisterDTO, Driver driver) {
        Map<String, DriverAttribute> newDriverAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverRegisterDTO.getDriverAttributes()) && !driverRegisterDTO.getDriverAttributes().isEmpty()) {
            driverRegisterDTO.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        }

        Map<String, DriverAttribute> oldDriverAttributeMap = new HashMap<>(8);
        try {
            List<DriverAttribute> byDriverId = driverAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, DriverAttribute> entry : newDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            DriverAttribute info = newDriverAttributeMap.get(name);
            info.setDriverId(driver.getId());
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(info);
            } else {
                log.debug("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.add(info);
            }
        }

        for (Map.Entry<String, DriverAttribute> entry : oldDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverInfoService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver info and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                    driverAttributeService.delete(oldDriverAttributeMap.get(name).getId());
                }
            }
        }
    }

    /**
     * 注册位号属性
     *
     * @param driverRegisterDTO DriverRegisterDTO
     * @param driver            Driver
     */
    private void registerPointAttribute(DriverRegisterDTO driverRegisterDTO, Driver driver) {
        Map<String, PointAttribute> newPointAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverRegisterDTO.getPointAttributes()) && !driverRegisterDTO.getPointAttributes().isEmpty()) {
            driverRegisterDTO.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        }

        Map<String, PointAttribute> oldPointAttributeMap = new HashMap<>(8);
        try {
            List<PointAttribute> byDriverId = pointAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, PointAttribute> entry : newPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            PointAttribute attribute = newPointAttributeMap.get(name);
            attribute.setDriverId(driver.getId());
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                log.debug("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(attribute);
            } else {
                log.debug("Point attribute registered, adding: {}", attribute);
                pointAttributeService.add(attribute);
            }
        }

        for (Map.Entry<String, PointAttribute> entry : oldPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointInfoService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point info and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.delete(oldPointAttributeMap.get(name).getId());
                }
            }
        }
    }

}