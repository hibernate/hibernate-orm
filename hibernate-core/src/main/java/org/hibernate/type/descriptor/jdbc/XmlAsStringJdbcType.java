/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Specialized type mapping for {@code SQLXML} and the XML SQL data type.
 *
 * @author Christian Beikov
 */
public class XmlAsStringJdbcType extends XmlJdbcType implements AdjustableJdbcType {
	/**
	 * Singleton access
	 */
	public static final XmlAsStringJdbcType VARCHAR_INSTANCE = new XmlAsStringJdbcType( SqlTypes.LONG32VARCHAR, null );
	public static final XmlAsStringJdbcType NVARCHAR_INSTANCE = new XmlAsStringJdbcType( SqlTypes.LONG32NVARCHAR, null );
	public static final XmlAsStringJdbcType CLOB_INSTANCE = new XmlAsStringJdbcType( SqlTypes.CLOB, null );
	public static final XmlAsStringJdbcType NCLOB_INSTANCE = new XmlAsStringJdbcType( SqlTypes.NCLOB, null );

	private final boolean nationalized;
	private final int ddlTypeCode;

	public XmlAsStringJdbcType(int ddlTypeCode, EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
		this.ddlTypeCode = ddlTypeCode;
		this.nationalized = ddlTypeCode == SqlTypes.LONG32NVARCHAR
				|| ddlTypeCode == SqlTypes.NCLOB;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new XmlAsStringJdbcType( ddlTypeCode, mappingType );
	}

	@Override
	public int getJdbcTypeCode() {
		return nationalized ? SqlTypes.NVARCHAR : SqlTypes.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public int getDdlTypeCode() {
		return ddlTypeCode;
	}

	@Override
	public String toString() {
		return "XmlAsStringJdbcType";
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		// Depending on the size of the column, we might have to adjust the jdbc type code for DDL.
		// In some DBMS we can compare LOBs with special functions which is handled in the SqlAstTranslators,
		// but that requires the correct jdbc type code to be available, which we ensure this way
		if ( getEmbeddableMappingType() == null ) {
			if ( needsLob( indicators ) ) {
				return indicators.isNationalized() ? NCLOB_INSTANCE : CLOB_INSTANCE;
			}
			else {
				return indicators.isNationalized() ? NVARCHAR_INSTANCE : VARCHAR_INSTANCE;
			}
		}
		else {
			if ( needsLob( indicators ) ) {
				return new XmlAsStringJdbcType(
						indicators.isNationalized() ? SqlTypes.NCLOB : SqlTypes.CLOB,
						getEmbeddableMappingType()
				);
			}
			else {
				return new XmlAsStringJdbcType(
						indicators.isNationalized() ? SqlTypes.LONG32NVARCHAR : SqlTypes.LONG32VARCHAR,
						getEmbeddableMappingType()
				);
			}
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
					final String xml = ( (XmlAsStringJdbcType) getJdbcType() ).toString(
							value,
							getJavaType(),
							options
					);
					if ( options.getDialect().supportsNationalizedMethods() ) {
						st.setNString( index, xml );
					}
					else {
						st.setString( index, xml );
					}
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final String xml = ( (XmlAsStringJdbcType) getJdbcType() ).toString(
							value,
							getJavaType(),
							options
					);
					if ( options.getDialect().supportsNationalizedMethods() ) {
						st.setNString( name, xml );
					}
					else {
						st.setString( name, xml );
					}
				}
			};
		}
		else {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final String xml = ( (XmlAsStringJdbcType) getJdbcType() ).toString(
							value,
							getJavaType(),
							options
					);
					st.setString( index, xml );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final String xml = ( (XmlAsStringJdbcType) getJdbcType() ).toString(
							value,
							getJavaType(),
							options
					);
					st.setString( name, xml );
				}
			};
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

				private X getObject(String xml, WrapperOptions options) throws SQLException {
					if ( xml == null ) {
						return null;
					}
					return ( (XmlAsStringJdbcType) getJdbcType() ).fromString(
							xml,
							getJavaType(),
							options
					);
				}
			};
		}
		else {
			return new BasicExtractor<>( javaType, this ) {

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return getObject( rs.getString( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return getObject( statement.getString( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return getObject( statement.getString( name ), options );
				}

				private X getObject(String xml, WrapperOptions options) throws SQLException {
					if ( xml == null ) {
						return null;
					}
					return ( (XmlAsStringJdbcType) getJdbcType() ).fromString(
							xml,
							getJavaType(),
							options
					);
				}
			};
		}
	}
}
