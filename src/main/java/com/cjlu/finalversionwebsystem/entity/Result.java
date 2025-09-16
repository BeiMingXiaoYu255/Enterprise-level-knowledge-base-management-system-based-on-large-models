package com.cjlu.finalversionwebsystem.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {
    int code;
    String msg;
    Object data;
    public static Result success()
    {
        return new Result(1,"success",null);
    }
    public static Result success(Object data)
    {
        return new Result(1,"success",data);
    }
    public static Result error(String msg,Object data)
    {
        return new Result(0,msg,data);
    }
    public static Result error(String msg)
    {
        return new Result(0,msg,null);
    }
}