/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.fallback.DriverClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.dto.DriverDto;
import io.github.pnoker.common.model.Driver;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * 驱动 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = ManagerServiceConstant.DRIVER_URL_PREFIX, name = ManagerServiceConstant.SERVICE_NAME, fallbackFactory = DriverClientFallback.class)
public interface DriverClient {

    /**
     * 新增 Driver
     *
     * @param driver   Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/add")
    R<Driver> add(@Validated(Insert.class) @RequestBody Driver driver, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除 Driver
     *
     * @param id 驱动ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Driver
     *
     * @param driver   Driver
     * @param tenantId 租户ID
     * @return Driver
     */
    @PostMapping("/update")
    R<Driver> update(@Validated(Update.class) @RequestBody Driver driver, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询 Driver
     *
     * @param id 驱动ID
     * @return Driver
     */
    @GetMapping("/id/{id}")
    R<Driver> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 集合查询 Driver
     *
     * @param driverIds Driver ID Set
     * @return Map String:Driver
     */
    @PostMapping("/ids")
    R<Map<String, Driver>> selectByIds(@RequestBody Set<String> driverIds);

    /**
     * 根据 SERVICENAME 查询 Driver
     *
     * @param serviceName Driver Service Name
     * @return Driver
     */
    @GetMapping("/service/{serviceName}")
    R<Driver> selectByServiceName(@NotNull @PathVariable(value = "serviceName") String serviceName);

    /**
     * 根据 TYPE 、 HOST 、 PORT 查询 Driver
     *
     * @param type     Driver type
     * @param host     Driver Host
     * @param port     Driver Port
     * @param tenantId 租户ID
     * @return Driver
     */
    @GetMapping("/type/{type}/host/{host}/port/{port}")
    R<Driver> selectByHostPort(@NotNull @PathVariable(value = "type") String type, @NotNull @PathVariable(value = "host") String host, @NotNull @PathVariable(value = "port") Integer port, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 模糊分页查询 Driver
     *
     * @param driverDto Driver Dto
     * @param tenantId  租户ID
     * @return Page Of Driver
     */
    @PostMapping("/list")
    R<Page<Driver>> list(@RequestBody(required = false) DriverDto driverDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}