package com.refit.app.global.util;

import com.refit.app.domain.memberProduct.model.UsageStatus;
import org.apache.ibatis.type.*;

import java.sql.*;

@MappedTypes(UsageStatus.class)
@MappedJdbcTypes({JdbcType.NUMERIC, JdbcType.DECIMAL, JdbcType.INTEGER, JdbcType.OTHER})
public class UsageStatusTypeHandler extends BaseTypeHandler<UsageStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UsageStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public UsageStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : UsageStatus.fromCode(code);
    }

    @Override
    public UsageStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : UsageStatus.fromCode(code);
    }

    @Override
    public UsageStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : UsageStatus.fromCode(code);
    }
}
