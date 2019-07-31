package com.nomad.dao;

import com.nomad.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Repository
//@Cacheable
public class JdbcUserRepository {

    private JdbcOperations jdbcOperations;

    @Autowired
    public JdbcUserRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Cacheable(value = "userCache",
            unless="#result.username.contains('tmp')",
            condition = "#id >= 10")
    public User findOne(int id) {
        return (User) jdbcOperations.query(
                "select * from user where id = ?",
                new RowMapper<Object>() {
                    @Override
                    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new User(rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("sex").charAt(0),
                                rs.getInt("age"));
                    }
                },
                id
        );
    }

    @CachePut(value = "userCache", key = "#result.id")
    public User addUser(User user) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("username", user.getUsername());
        paramMap.put("sex", user.getSex());
        paramMap.put("age", user.getAge());

        jdbcOperations.update(
                "insert into user(username, sex, age) values(:username, :sex, :age);",   //使用命名参数  参数值不用一一对应
                paramMap
        );
        return user;
    }

    @CacheEvict(value = "userCache",
            allEntries = false,
            beforeInvocation = false)
    public void remove(int id) {
        jdbcOperations.update("delete from User where id = ?", id);
    }

}
