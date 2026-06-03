package com.example.experiment.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

/**
 * UUID String ↔ BINARY(16) 类型处理器
 * 全局注册（type-handlers-package），自动按需处理：
 * - 写入：32位十六进制字符串 → 16字节 BINARY
 * - 读取：16字节 BINARY → 32位十六进制字符串
 * - 非UUID字段（如 username、password）自动回退
 */
public class UUIDTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter.matches("[0-9a-fA-F]{32}")) {
            UUID uuid = UUID.fromString(
                    parameter.replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5"
                    )
            );
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            ps.setBytes(i, bb.array());
        } else {
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return bytes == null || bytes.length != 16
                ? rs.getString(columnName)
                : bytesToUuidString(bytes);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return bytes == null || bytes.length != 16
                ? rs.getString(columnIndex)
                : bytesToUuidString(bytes);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return bytes == null || bytes.length != 16
                ? cs.getString(columnIndex)
                : bytesToUuidString(bytes);
    }

    private String bytesToUuidString(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low).toString().replace("-", "");
    }
}