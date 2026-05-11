# 1 新增员工里，用户已存在的duplicate handler
```java
public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
//Duplicate entry 'ik' for key 'employee.idx_username'
String message = ex.getMessage();
if(message.contains("Duplicate entry")){
String[] split = message.split("");
String username = split[2];
String msg = username + MessageConstant.ALREADY;
return Result.error(msg);
}
else{
return Result.error(MessageConstant.UNKNOWN_ERROR);        }

    }
```
