package com.cjlu.finalversionwebsystem.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理List<String>与数据库TEXT类型的转换
 */
public class ListToStringTypeHandler extends BaseTypeHandler<List<String>> {

    // 将List<String>转为字符串存入数据库（用逗号分隔）
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null && !parameter.isEmpty()) {
            String value = String.join(",", parameter);
            ps.setString(i, value);
        } else {
            ps.setString(i, null);
        }
    }

    // 从数据库读取字符串转为List<String>
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convertStringToList(value);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convertStringToList(value);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convertStringToList(value);
    }

    // 辅助方法：字符串转List
    private List<String> convertStringToList(String value) {
        if (value == null || value.isEmpty()) {
            return List.of(); // 返回空列表而非null，避免NPE
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}