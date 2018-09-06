package com.sissi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Sissi on 2018/9/5.
 */
// 定义生成类的信息. 生成的类保存了"请求-响应-超时"以及"响应-响应类"的映射关系
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface GenFile {
    String packageName(); // 生成的java类所在的包名
    String className(); // 生成的java类名.
}
