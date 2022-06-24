/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.data.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.data.feign.PointValueClient;
import com.dc3.common.bean.R;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.dto.PointValueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PointValueClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class PointValueClientFallback implements FallbackFactory<PointValueClient> {

    @Override
    public PointValueClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-DATA" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new PointValueClient() {

            @Override
            public R<List<PointValue>> latest(String deviceId, Boolean history) {
                return R.fail(message);
            }

            @Override
            public R<PointValue> latest(String deviceId, String pointId, Boolean history) {
                return R.fail(message);
            }

            @Override
            public R<Page<PointValue>> list(PointValueDto pointValueDto) {
                return R.fail(message);
            }

        };
    }
}