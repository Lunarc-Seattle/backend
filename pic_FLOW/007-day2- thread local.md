
2 ![img_18.png](img_18.png)


![img_20.png](img_20.png)

## 逻辑
![img_21.png](img_21.png)

![img_24.png](img_24.png)
### 解析出登录员工的id后，如何传递给service的save方法？

### ThreadLocal** 是解决“谁在操作数据库”这个问题的核心武器

你可以把 **ThreadLocal** 理解为每个线程（Request 请求）专属的一个“私人保险柜”。

---

### 1. 核心定义

**ThreadLocal** 并不是一个线程，而是线程的一个**局部变量**。

* **线程隔离**：它为每个执行任务的线程提供了一份独立的存储空间。
* **只有你能看**：存在这个保险柜里的东西，只有当前的这个线程能看到和修改，其他线程（即使同时在运行）完全看不见。

---

### 2. 生动的比喻：健身房的私人储物柜

想象一个健身房（我们的后端程序）：

* **线程（Thread）**：就是进入健身房锻炼的**顾客**。
* **代码逻辑**：就是健身房里的**运动器材**。所有顾客都用同样的器材（执行同样的代码）。
* **ThreadLocal**：就是健身房里的**私人储物柜**。

**工作流程：**

1. 顾客 A 进门（请求开始），把自己的手机（用户 ID）存进自己的 1 号柜。
2. 顾客 B 进门，把自己的手机（另一个用户 ID）存进自己的 2 号柜。
3. 当他们在跑步机上锻炼（执行 Service 层代码）时，如果想看时间，只需去**自己的柜子**里拿手机。
4. **重点**：顾客 A 绝对拿不到顾客 B 的手机，他们互不干扰。

---

### 3. 为什么在项目中非要用它？（解决“参数地狱”）

在[新增员工](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D20)的功能里，我们需要在 `Service` 层知道当前登录人的 ID，以便填入 `createUser` 字段。

* **笨办法（不推荐）**：在每个方法（Controller -> Service -> Mapper）的参数里都传一个 `Long empId`。
* *代价*：如果项目有 100 个方法要用这个 ID，你得改 100 遍代码，非常痛苦。


* **聪明办法（ThreadLocal）**：
1. 在 **拦截器 (Interceptor)** 校验 JWT 令牌通过后，直接把解析出来的 `empId` 存入当前线程的 `ThreadLocal`。
2. 后续无论在哪个类、哪个方法里，只要还在同一个线程内，随时随地用 `BaseContext.getCurrentId()` 就能取出来。



---

### 4. 在代码里怎么用？

在你的项目中，黑马程序员提供了一个工具类叫做 `BaseContext`：

```java
public class BaseContext {
    // 创建一个 ThreadLocal 容器
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 存数据（一般在拦截器里干）
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    // 取数据（在 Service 补全 Entity 时干）
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    // 删数据（防止内存泄漏）
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}

```

---

### 📝 深度总结

* **生命周期**：在一次 HTTP 请求中，Spring Boot 会分配一个线程来处理。这个线程从接收请求到返回响应，这一路上的所有代码操作，共享同一个 `ThreadLocal` 空间。
* **安全性**：它保证了用户 A 的信息绝不会被错扣在用户 B 的头上。

你现在是不是正准备把拦截器里解析出来的 `empId` 通过 `BaseContext` 存起来？在这一步有没有遇到 `id` 拿出来是 `null` 的情况？

# 具体代码修改的部分是：
```java
public void save(EmployeeDTO employeeDTO) {
Employee employee = new Employee();
//对象属性拷贝
//                          源头    --     拷贝对象
BeanUtils.copyProperties(employeeDTO,employee);
// employee 里的属性比employeeDTO还有更多。所以需要设置

        employee.setStatus(StatusConstant.ENABLE);
        //设置账号的状态，默认正常状态 1表示正常 0为锁定

        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //设置密码，默认123456

        //设置当前记录的创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //设置当前记录创建人的id
        //TODO 后期改为当前登录用户的id 因为现在不知道登陆人的id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

}

```
```java
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    //判断当前拦截到的是Controller的方法还是其他资源
    if (!(handler instanceof HandlerMethod)) {
        //当前拦截到的不是动态方法，直接放行
        return true;
    }

    //1、从请求头中获取令牌
    String token = request.getHeader(jwtProperties.getAdminTokenName());

    //2、校验令牌
    try {
        log.info("jwt校验:{}", token);
        Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
        Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
        log.info("当前员工id：", empId);
        BaseContext.setCurrentId(empId);
        //使用thread local
        //3、通过，放行
        return true;
    } catch (Exception ex) {
        //4、不通过，响应401状态码
        response.setStatus(401);
        return false;
    }
}
```