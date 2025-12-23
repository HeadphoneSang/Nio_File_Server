package org.template.server.components.abstracts;

import java.lang.annotation.*;

@Target(ElementType.FIELD)          // 注解作用位置
@Retention(RetentionPolicy.RUNTIME) // 运行期可反射读取
@Documented
public @interface ToByte {
}