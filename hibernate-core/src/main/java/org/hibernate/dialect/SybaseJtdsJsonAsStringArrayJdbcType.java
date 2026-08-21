/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JsonAsStringArrayJdbcType;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specialized type mapping for {@code JSON} that binds UTF-16LE bytes,
 * because the jTDS driver can't handle Unicode characters and doesn't support nationalized methods.
 * For extraction, the {@code getString} method works fine though.
 */
public class SybaseJtdsJsonAsStringArrayJdbcType extends JsonAsStringArrayJdbcType {

	public SybaseJtdsJsonAsStringArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType, SqlTypes.NCLOB );
	}

	public SybaseJtdsJsonAsStringArrayJdbcType(JdbcType elementJdbcType, int ddlTypeCode) {
		super( elementJdbcType, ddlTypeCode );
	}

	@Override
	public String toString() {
		return "SybaseJtdsXmlAsStringArrayJdbcType";
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		// Depending on the size of the column, we might have to adjust the jdbc type code for DDL.
		// In some DBMS we can compare LOBs with special functions which is handled in the SqlAstTranslators,
		// but that requires the correct jdbc type code to be available, which we ensure this way
		if ( needsLob( indicators ) ) {
			return indicators.isNationalized()
					? new SybaseJtdsJsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.NCLOB )
					: new SybaseJtdsJsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.CLOB );
		}
		else {
			return indicators.isNationalized()
					? new SybaseJtdsJsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.LONG32NVARCHAR )
					: new SybaseJtdsJsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.LONG32VARCHAR );
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		if ( !isNationalized() ) {
			return super.getBinder( javaType );
		}
		return new BasicBinder<>( javaType, this ) {

			private SybaseJtdsJsonAsStringArrayJdbcType getJsonAsStringArrayJdbcType() {
				return (SybaseJtdsJsonAsStringArrayJdbcType) getJdbcType();
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final SybaseJtdsJsonAsStringArrayJdbcType jdbcType = getJsonAsStringArrayJdbcType();
				final String xml = jdbcType.toString( value, getJavaType(), options );
				st.setBytes( index, xml.getBytes( StandardCharsets.UTF_16LE ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final SybaseJtdsJsonAsStringArrayJdbcType jdbcType = getJsonAsStringArrayJdbcType();
				final String xml = jdbcType.toString( value, getJavaType(), options );
				st.setBytes( name, xml.getBytes( StandardCharsets.UTF_16LE ) );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, SqlTypes.VARCHAR );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options)
					throws SQLException {
				st.setNull( name, SqlTypes.VARCHAR );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( !isNationalized() ) {
			return super.getExtractor( javaType );
		}
		return new BasicExtractor<>( javaType, this ) {

			private SybaseJtdsJsonAsStringArrayJdbcType getJsonAsStringArrayJdbcType() {
				return (SybaseJtdsJsonAsStringArrayJdbcType) getJdbcType();
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final String value = rs.getString( paramIndex );
				return getJsonAsStringArrayJdbcType().fromString( value, getJavaType(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				final String value = statement.getString( index );
				return getJsonAsStringArrayJdbcType().fromString( value, getJavaType(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				final String value = statement.getString( name );
				return getJsonAsStringArrayJdbcType().fromString( value, getJavaType(), options );
			}

		};
	}
}
