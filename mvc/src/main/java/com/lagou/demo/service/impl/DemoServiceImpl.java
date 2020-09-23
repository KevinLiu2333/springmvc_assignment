package com.lagou.demo.service.impl;

import com.lagou.demo.service.IDemoService;
import com.lagou.edu.annotations.LagouService;

/**
 * Created with IntelliJ IDEA.
 * User: liuku
 * Date: 2020/9/18
 * Time: 22:00
 *
 * @author liuku
 */
@LagouService
public class DemoServiceImpl implements IDemoService {

    @Override
    public String get(String name) {
        System.out.println("service 实现类中的name参数:" + name);
        return name;
    }
}
