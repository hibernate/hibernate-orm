/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Specialized type mapping for {@code JSON_ARRAY} and the JSON SQL data type.
 *
 * @author Christian Beikov
 */
public class JsonAsStringArrayJdbcType extends JsonArrayJdbcType implements AdjustableJdbcType {

	private final boolean nationalized;
	private final int ddlTypeCode;

	public JsonAsStringArrayJdbcType(JdbcType elementJdbcType) {
		this( elementJdbcType, SqlTypes.LONG32VARCHAR );
	}

	public JsonAsStringArrayJdbcType(JdbcType elementJdbcType, int ddlTypeCode) {
		super( elementJdbcType );
		this.ddlTypeCode = ddlTypeCode;
		this.nationalized = ddlTypeCode == SqlTypes.LONG32NVARCHAR
				|| ddlTypeCode == SqlTypes.NCLOB;
	}

	@Override
	public int getJdbcTypeCode() {
		return nationalized ? SqlTypes.NVARCHAR : SqlTypes.VARCHAR;
	}

	@Override
	public int getDdlTypeCode() {
		return ddlTypeCode;
	}

	@Override
	public String toString() {
		return "JsonArrayAsStringJdbcType";
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		// Depending on the size of the column, we might have to adjust the jdbc type code for DDL.
		// In some DBMS we can compare LOBs with special functions which is handled in the SqlAstTranslators,
		// but that requires the correct jdbc type code to be available, which we ensure this way
		if ( needsLob( indicators ) ) {
			return indicators.isNationalized()
					? new JsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.NCLOB )
					: new JsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.CLOB );
		}
		else {
			return indicators.isNationalized()
					? new JsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.LONG32NVARCHAR )
					: new JsonAsStringArrayJdbcType( getElementJdbcType(), SqlTypes.LONG32VARCHAR );
		}
	}

	protected boolean needsLob(JdbcTypeIndicators indicators) {
		final Dialect dialect = indicators.getDialect();
		final long length = indicators.getColumnLength();
		final long maxLength = indicators.isNationalized()
				? dialect.getMaxNVarcharLength()
				: dialect.getMaxVarcharLength();
		if ( length > maxLength ) {
			return true;
		}

		final DdlTypeRegistry ddlTypeRegistry = indicators.getTypeConfiguration().getDdlTypeRegistry();
		final String typeName = ddlTypeRegistry.getTypeName( getDdlTypeCode(), dialect );
		return typeName.equals( ddlTypeRegistry.getTypeName( SqlTypes.CLOB, dialect ) )
				|| typeName.equals( ddlTypeRegistry.getTypeName( SqlTypes.NCLOB, dialect ) );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		if ( nationalized ) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final String json = ( (JsonAsStringArrayJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
					if ( options.getDialect().supportsNationalizedMethods() ) {
						st.setNString( index, json );
					}
					else {
						st.setString( index, json );
					}
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final String json = ( (JsonAsStringArrayJdbcType) getJdbcType() ).toString( value, getJavaType(), options );
					if ( options.getDialect().supportsNationalizedMethods() ) {
						st.setNString( name, json );
					}
					else {
						st.setString( name, json );
					}
				}
			};
		}
		else {
			return super.getBinder( javaType );
		}
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( nationalized ) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					if ( options.getDialect().supportsNationalizedMethods() ) {
						return getObject( rs.getNString( paramIndex ), options );
					}
					else {
						return getObject( rs.getString( paramIndex ), options );
					}
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					if ( options.getDialect().supportsNationalizedMethods() ) {
						return getObject( statement.getNString( index ), options );
					}
					else {
						return getObject( statement.getString( index ), options );
					}
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					if ( options.getDialect().supportsNationalizedMethods() ) {
						return getObject( statement.getNString( name ), options );
					}
					else {
						return getObject( statement.getString( name ), options );
					}
				}

				private X getObject(String json, WrapperOptions options) throws SQLException {
					return ( (JsonAsStringArrayJdbcType) getJdbcType() ).fromString(
							json,
							getJavaType(),
							options
					);
				}

			};
		}
		else {
			return super.getExtractor( javaType );
		}
	}
}
