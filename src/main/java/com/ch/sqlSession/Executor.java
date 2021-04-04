package com.ch.sqlSession;

import com.ch.pojo.Configuration;
import com.ch.pojo.MappedStatement;

import java.sql.SQLException;
import java.util.List;

public interface Executor {
    public <E> List<E> query(Configuration configuration, MappedStatement mappedStatement,Object... params) throws SQLException, Exception;
}
