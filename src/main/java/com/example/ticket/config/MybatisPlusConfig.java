package com.example.ticket.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
@Slf4j
@MapperScan("com.example.ticket.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }


    @Bean
    public MetaObjectHandler metaObjectHandler(){
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.setFieldValByName("createTime",new Date(),metaObject);
                this.setFieldValByName("updateTime",new Date(),metaObject);
                // version 和 deleted 字段仅在实体类定义了这些字段时才填充
                // 当前实体类未使用这些字段，所以注释掉
                // this.setFieldValByName("version",1,metaObject);
                // this.setFieldValByName("deleted",0,metaObject);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.setFieldValByName("updateTime",new Date(),metaObject);
            }
        };
    }


}
