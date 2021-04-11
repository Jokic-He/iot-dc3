/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
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

package com.dc3.common.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.dc3.common.valid.Insert;
import com.dc3.common.valid.Update;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 设备表
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Device extends Description {

    @NotBlank(message = "name can't be empty", groups = {Insert.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_]{1,31}$", message = "invalid name,contains invalid characters or length is not in the range of 2~32", groups = {Insert.class, Update.class})
    private String name;

    /**
     * 是否结构化存储数据
     * 默认：false，为单点存储
     */
    private Boolean multi;

    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull(message = "profile id can't be empty", groups = {Insert.class, Update.class})
    private Long profileId;

    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull(message = "group id can't be empty", groups = {Insert.class, Update.class})
    private Long groupId;

    @TableField(exist = false)
    private String status;

    public Device(String name, Long profileId, Long groupId) {
        super();
        this.name = name;
        this.profileId = profileId;
        this.groupId = groupId;
    }
}
