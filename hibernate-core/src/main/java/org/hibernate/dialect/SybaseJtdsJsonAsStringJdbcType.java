/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;

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
public class SybaseJtdsJsonAsStringJdbcType extends JsonAsStringJdbcType {

	public static final SybaseJtdsJsonAsStringJdbcType JTDS_VARCHAR_INSTANCE =
			new SybaseJtdsJsonAsStringJdbcType( SqlTypes.LONG32VARCHAR, null );
	public static final SybaseJtdsJsonAsStringJdbcType JTDS_NVARCHAR_INSTANCE =
			new SybaseJtdsJsonAsStringJdbcType( SqlTypes.LONG32NVARCHAR, null );
	public static final SybaseJtdsJsonAsStringJdbcType JTDS_CLOB_INSTANCE =
			new SybaseJtdsJsonAsStringJdbcType( SqlTypes.CLOB, null );
	public static final SybaseJtdsJsonAsStringJdbcType JTDS_INSTANCE =
			new SybaseJtdsJsonAsStringJdbcType( SqlTypes.NCLOB, null );

	public SybaseJtdsJsonAsStringJdbcType(int ddlTypeCode, EmbeddableMappingType embeddableMappingType) {
		super( ddlTypeCode, embeddableMappingType );
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new SybaseJtdsJsonAsStringJdbcType( getDdlTypeCode(), mappingType );
	}

	@Override
	public String toString() {
		return "SybaseJtdsJsonAsStringJdbcType";
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		// Depending on the size of the column, we might have to adjust the jdbc type code for DDL.
		// In some DBMS we can compare LOBs with special functions which is handled in the SqlAstTranslators,
		// but that requires the correct jdbc type code to be available, which we ensure this way
		if ( getEmbeddableMappingType() == null ) {
			if ( needsLob( indicators ) ) {
				return indicators.isNationalized() ? JTDS_INSTANCE : JTDS_CLOB_INSTANCE;
			}
			else {
				return indicators.isNationalized() ? JTDS_NVARCHAR_INSTANCE : JTDS_VARCHAR_INSTANCE;
			}
		}
		else {
			if ( needsLob( indicators ) ) {
				return new SybaseJtdsJsonAsStringJdbcType(
						indicators.isNationalized() ? SqlTypes.NCLOB : SqlTypes.CLOB,
						getEmbeddableMappingType()
				);
			}
			else {
				return new SybaseJtdsJsonAsStringJdbcType(
						indicators.isNationalized() ? SqlTypes.LONG32NVARCHAR : SqlTypes.LONG32VARCHAR,
						getEmbeddableMappingType()
				);
			}
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		if ( !isNationalized() ) {
			return super.getBinder( javaType );
		}
		return new BasicBinder<>( javaType, this ) {

			private SybaseJtdsJsonAsStringJdbcType getJsonAsStringJdbcType() {
				return (SybaseJtdsJsonAsStringJdbcType) getJdbcType();
			}

			private String getXml(X value, WrapperOptions options) throws SQLException {
				return getJsonAsStringJdbcType().toString( value, getJavaType(), options );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final String xml = getXml( value, options );
				st.setBytes( index, xml.getBytes( StandardCharsets.UTF_16LE ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final String xml = getXml( value, options );
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

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return getObject( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getObject( statement.getString( name ), options );
			}

			private X getObject(String xml, WrapperOptions options) throws SQLException {
				return xml == null ? null : getJsonAsStringJdbcType().fromString( xml, getJavaType(), options );
			}

			private SybaseJtdsJsonAsStringJdbcType getJsonAsStringJdbcType() {
				return (SybaseJtdsJsonAsStringJdbcType) getJdbcType();
			}
		};
	}
}
