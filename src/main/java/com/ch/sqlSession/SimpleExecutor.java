package com.ch.sqlSession;

import com.ch.config.BoundSql;
import com.ch.pojo.Configuration;
import com.ch.pojo.MappedStatement;
import com.ch.utils.GenericTokenParser;
import com.ch.utils.ParameterMapping;
import com.ch.utils.ParameterMappingTokenHandler;
import com.ch.utils.TokenHandler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class SimpleExecutor implements Executor{                                                  //user
    public <E> List<E> query(Configuration configuration, MappedStatement mappedStatement, Object... params) throws Exception {
        //1.注册驱动 获取连接
     //   Class.forName(configuration.getDataSource().)
        Connection connection = configuration.getDataSource().getConnection();
        //获取sql语句:  select * from user where id=#{id} and username=#{username}
        //转换sql语句：select * from user where id=? and username=?,还
        String sql=mappedStatement.getSql();
        BoundSql boundSql=getBoundSql(sql);
     //3.获取预处理对象：preparedStatement
        PreparedStatement preparedStatement = connection.prepareStatement(boundSql.getSqlText());

        //4.设置参数
        //String resultType = mappedStatement.getResultType();
        String parameterType = mappedStatement.getParameterType();
        Class<?> parametertypeClass=getClassType(parameterType);
        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappingList();
        for (int i = 0; i <parameterMappingList.size() ; i++) {
            ParameterMapping parameterMapping = parameterMappingList.get(i);
            String content = parameterMapping.getContent();
            //反射
            Field declaredField = parametertypeClass.getDeclaredField(content);
            //暴力访问
            declaredField.setAccessible(true);
            Object o = declaredField.get(params[0]);

            preparedStatement.setObject(i+1,o);

        }
        //5.执行sql
        ResultSet resultSet = preparedStatement.executeQuery();
        String resultType = mappedStatement.getResultType();
        Class<?> resulttypeClass = getClassType(resultType);


        ArrayList<Object> objects = new ArrayList();
        //6.封装返回结果集
        while (resultSet.next()){
            //元数据
            Object o = resulttypeClass.newInstance();
            ResultSetMetaData metaData = resultSet.getMetaData();

            for (int i = 1; i < metaData.getColumnCount()+1; i++) {
               //字段名
                String columnName = metaData.getColumnName(i);
                //字段值
                Object value = resultSet.getObject(columnName);

                //使用反射或者内省，根据数据库表和实体的对应关系，完成封装
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(columnName, resulttypeClass);
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(o,value);

            }

            objects.add(o);
        }

        return (List<E>) objects;
    }

    private Class<?> getClassType(String parameterType) throws ClassNotFoundException {
        if (parameterType!=null){
            Class<?> aClass = Class.forName(parameterType);
            return aClass;
        }
        return null;
    }

    /*完成对#{}的解析工作  1.将#{}使用?进行代替，2.解析出#{}里面的值进行存储
    @param sql
    * */
    private BoundSql getBoundSql(String sql) {
        //标记处理类，配合标记解析器来完成对占位符的解析
        ParameterMappingTokenHandler parameterMappingTokenHandler = new ParameterMappingTokenHandler();
        GenericTokenParser genericTokenParser = new GenericTokenParser("#{", "}", parameterMappingTokenHandler);
        //解析出来的sql
        String parseSql = genericTokenParser.parse(sql);
        //#{}解析出来的参数名称
        List<ParameterMapping> parameterMappings = parameterMappingTokenHandler.getParameterMappings();
        BoundSql boundSql = new BoundSql(parseSql,parameterMappings);
        return  boundSql;
    }
}
