/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.StructJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * DB2 supports UDTs but not in JDBC, so there is a feature called "transforms",
 * which allows to specify an automatic translation from one data type to another.
 * To support UDTs, we require that a transform exists for the UDT that serializes from SQL to XML
 * and deserializes to SQL from UDT. This means that from the JDBC perspective, this is an XML type,
 * but the database models it internally as UDT.
 * <p>
 * The {@link org.hibernate.dialect.aggregate.DB2AggregateSupport} generates the functions and transforms for this
 * process automatically, but note that all of this is only used for functions and native queries.
 * By default, we select individual struct parts to avoid the encoding/decoding.
 *
 * @author Christian Beikov
 */
public class DB2StructJdbcType implements StructJdbcType {

	public static final DB2StructJdbcType INSTANCE = new DB2StructJdbcType();

	private final String structTypeName;
	private final EmbeddableMappingType embeddableMappingType;
	private final ValueExtractor<Object[]> objectArrayExtractor;

	private DB2StructJdbcType() {
		// The default instance is for reading only and will return an Object[]
		this( null, null );
	}

	public DB2StructJdbcType(EmbeddableMappingType embeddableMappingType, String structTypeName) {
		this.embeddableMappingType = embeddableMappingType;
		this.structTypeName = structTypeName;
		// We cache the extractor for Object[] here
		// since that is used in AggregateEmbeddableFetchImpl and AggregateEmbeddableResultImpl
		this.objectArrayExtractor = createBasicExtractor( new UnknownBasicJavaType<>( Object[].class ) );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.STRUCT;
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new DB2StructJdbcType( mappingType, sqlType );
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	@Override
	public String getStructTypeName() {
		return structTypeName;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		if ( embeddableMappingType == null ) {
			return typeConfiguration.getJavaTypeRegistry().getDescriptor( Object[].class );
		}
		else {
			//noinspection unchecked
			return (JavaType<T>) embeddableMappingType.getMappedJavaType();
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setSQLXML( index, createJdbcValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setSQLXML( name, createJdbcValue( value, options ) );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		if ( javaType.getJavaTypeClass() == Object[].class ) {
			//noinspection unchecked
			return (ValueExtractor<X>) objectArrayExtractor;
		}
		return createBasicExtractor( javaType );
	}

	private <X> BasicExtractor<X> createBasicExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getValue( rs.getSQLXML( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getValue( statement.getSQLXML( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getValue( statement.getSQLXML( name ), options );
			}

			private X getValue(SQLXML object, WrapperOptions options) throws SQLException {
				if ( object == null ) {
					return null;
				}
				return XmlHelper.fromString(
						embeddableMappingType,
						object.getString(),
						javaType.getJavaTypeClass() != Object[].class,
						options
				);
			}
		};
	}

	@Override
	public SQLXML createJdbcValue(Object value, WrapperOptions options) throws SQLException {
		final SQLXML sqlxml = options.getSession()
				.getJdbcCoordinator()
				.getLogicalConnection()
				.getPhysicalConnection()
				.createSQLXML();
		sqlxml.setString( XmlHelper.toString( embeddableMappingType, value, options) );
		return sqlxml;
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		return XmlHelper.fromString( embeddableMappingType, (String) rawJdbcValue, false, options );
	}

}
