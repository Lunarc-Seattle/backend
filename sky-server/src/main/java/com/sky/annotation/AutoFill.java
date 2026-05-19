package com.sky.annotation;

import com.sky.enumeration.OperationType;
//引入枚举 OperationType（项目里定义的，应该只有 INSERT 和 UPDATE 两个值）。

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理
 */
@Target(ElementType.METHOD)
//这个注解只能贴在方法上（不能贴在类、字段、参数上）。
//
//  - ElementType.FIELD —— 字段
//  - ElementType.METHOD —— 方法 ← 这里用的
//  - ElementType.PARAMETER —— 参数
@Retention(RetentionPolicy.RUNTIME)
// 保留策略 = 运行时可见。三个级别：
//  ┌────────┬────────────────────────┬───────────┐
//  │  级别  │             含义              │   例子    │
//  ├────────┼───────────────────────────────┼───────────┤
//  │ SOURCE  │ 只在源码里，编译后丢掉        │ @Override                                  │
//  ├─────────┼───────────────────────────────┼────────────────────────────────────────────┤
//  │ CLASS   │ 编译进 .class，但运行时拿不到 │ （默认）                                   │
//  ├─────────┼───────────────────────────────┼────────────────────────────────────────────┤
//  │ RUNTIME │ 运行时反射能拿到              │ ← 必须这个，AOP 才能在运行时通过反射检测到 │
//  └─────────┴───────────────────────────────┴────────────────────────────────────────────┘
public @interface AutoFill { //- @interface 表示这是一个注解类型（不是 interface多了个 @）
    //因为只有 update和insert需要公共字符段
    OperationType value();
    //里面一个属性 value()，类型是 OperationType 枚举
    //属性名叫 value 是特殊命名：使用时可以省略 value =，写 @AutoFill(OperationType.INSERT) 即可；如果叫别的名字（比如 type），就得写 @AutoFill(type =
    //  OperationType.INSERT)
}
//这是注解特有的语法，长得像方法但其实是属性。 这是 Java 里仅在 @interface 注解类型中才能用的写法，不是普通类的属性写法。
//
//  同一段代码，三种不同的"身份"
//
//  OperationType value();
//
//  这一行根据使用场景，扮演三种角色：
//
//  ┌────────────┬────────────────────────┬──────────────────────────────────┐
//  │    场景    │                长什么样                 │           它扮演的角色           │
//  ├────────────┼─────────────────────────────────────────┼──────────────────────────────────┤
//  │ 定义注解时 │ OperationType value();                  │ 像个抽象方法（没方法体，结尾 ;） │
//  ├────────────┼─────────────────────────────────────────┼──────────────────────────────────┤
//  │ 使用注解时 │ @AutoFill(value = OperationType.INSERT) │ 像个属性赋值（属性名 = 值）      │
//  ├────────────┼─────────────────────────────────────────┼──────────────────────────────────┤
//  │ 反射读取时 │ annotation.value()                      │ 像调用方法（拿到那个值）         │
//  └────────────┴─────────────────────────────────────────┴──────────────────────────────────┘
//
//  同一个 value，三种姿态——这就是注解语法的特殊性。
//
//  跟普通类的属性对比
//
//  普通 Java 类定义属性是这样：
//
//  public class Employee {
//      private String name;          // 字段（field / property）
//      private OperationType type;   // 字段
//  }
//
//  注解定义属性是这样：
//
//  public @interface AutoFill {
//      OperationType value();        // 注解元素（annotation element）
//      String description() default ""; // 带默认值的注解元素
//  }
//
//  ┌────────┬────────────┬────────────┐
//  │  维度  │    普通类属性    │  注解元素  │
//  ├────────┼──────────────────┼────────────┤
//  │ 关键字   │ class              │ @interface         │
//  ├──────────┼────────────────────┼────────────────────┤
//  │ 写法     │ private 类型 名;   │ 类型 名();         │
//  ├──────────┼────────────────────┼────────────────────┤
//  │ 有 () 吗 │ 没有               │ 有（虽然不是方法） │
//  ├──────────┼────────────────────┼────────────────────┤
//  │ 默认值   │ private int x = 5; │ int x() default 5;    │
//  ├──────────┼────────────────────┼───────────────────────┤
//  │ 赋值方式 │ obj.setX(5)        │ @注解(x = 5)          │
//  ├──────────┼────────────────────┼───────────────────────┤
//  │ 读取方式 │ obj.getX()         │ annotation.x() 或反射 │
//  └──────────┴────────────────────┴───────────────────────┘
//
//  为什么注解要用这种"假装是方法"的语法
//
//  这是 JDK 设计上的选择。@interface 底层其实是一个继承自 java.lang.annotation.Annotation
//  的接口，而接口里成员只能是"抽象方法"。所以注解的属性被设计成抽象方法的形式，但编译器和 JVM 会特殊对待它——把它当属性用。
//
//  你可以理解为："注解 = 一个特殊的接口，它的'抽象方法'其实是属性"。
//
//  在 AutoFill 里具体怎么用
//
//  定义（AutoFill.java）：
//  public @interface AutoFill {
//      OperationType value();   // ← 一个名为 value 的属性，类型是 OperationType
//  }
//
//  使用（在 Mapper 方法上贴）：
//
//  void insert(Employee employee);
//
//  // 或者因为属性名叫 value 比较特殊，可以省略：
//  @AutoFill(OperationType.INSERT)           // 等价写法
//  void insert(Employee employee);
//
//  读取（在 AOP 切面里）：
//
//  // 反射拿到方法上的 @AutoFill 注解
//  AutoFill autoFill = method.getAnnotation(AutoFill.class);
//
//  // 调用 value() 拿到属性值（这时它真的"像方法"了）
//  OperationType type = autoFill.value();   // 返回 INSERT 或 UPDATE
//  if (type == OperationType.INSERT) {
//      // 填充 createTime / createUser / updateTime / updateUser
//  } else if (type == OperationType.UPDATE) {
//      // 只填充 updateTime / updateUser
//  }
//
//  一句话记住
//
//  ▎ 在 @interface 里，类型 名(); 不是定义方法，是定义这个注解能带哪些"参数"。它形式上像方法、用起来像属性、反射读时又像方法——所以语法长得怪是正常的。
