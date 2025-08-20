package com.refit.app.global.util;

import org.apache.ibatis.type.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MappedTypes(List.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.CHAR, JdbcType.CLOB})
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        String joined = (parameter == null) ? null
                : parameter.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(", "));
        ps.setString(i, joined);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex));
    }

    private List<String> toList(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        return Stream.of(s.split("\\s*,\\s*"))
                .filter(t -> !t.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
