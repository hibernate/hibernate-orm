/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import oracle.jdbc.OracleType;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.OsonDocumentWriter;
import org.hibernate.type.format.jackson.JacksonOsonFormatMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * Type mapping of (JSON) array of JSON SQL data type for Oracle database.
 * This implementation is used when Jackson mapper is used and that the JDBC OSON extension
 * is available.
 *
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class OracleOsonJacksonArrayJdbcType extends OracleJsonArrayJdbcType {


	private static final Class osonFactoryKlass;

	static {
		try {
			osonFactoryKlass = JacksonOsonFormatMapper.class.getClassLoader().loadClass( "oracle.jdbc.provider.oson.OsonFactory" );
		}
		catch (ClassNotFoundException | LinkageError e) {
			// should not happen as OracleOsonJacksonArrayJdbcType is loaded
			// only when an Oracle OSON JDBC extension is present
			// see OracleDialect class.
			throw new ExceptionInInitializerError( "OracleOsonJacksonArrayJdbcType class loaded without OSON extension: " + e.getClass()+ " " + e.getMessage());
		}
	}


	public OracleOsonJacksonArrayJdbcType(JdbcType elementJdbcType) {
		super(elementJdbcType);
	}


	@Override
	public String toString() {
		return "OracleOsonJacksonArrayJdbcType";
	}


	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {

		return new BasicBinder<>( javaType, this ) {

			private <X> byte[] toOsonStream(X value, JavaType<X> javaType, WrapperOptions options) throws Exception {
				final Object[] domainObjects = javaType.unwrap( value, Object[].class, options );

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try (OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out )) {
					OsonDocumentWriter writer = new OsonDocumentWriter( generator );

					if ( getElementJdbcType() instanceof JsonJdbcType jsonElementJdbcType ) {
						final EmbeddableMappingType embeddableMappingType = jsonElementJdbcType.getEmbeddableMappingType();
						JsonHelper.serializeArray( embeddableMappingType, domainObjects, options, writer );
					}
					else {
						assert !( getElementJdbcType() instanceof AggregateJdbcType );
						final JavaType<?> elementJavaType = ( (BasicPluralJavaType<?>) javaType ).getElementJavaType();
						JsonHelper.serializeArray(
								elementJavaType,
								getElementJdbcType(),
								domainObjects,
								options,
								writer
						);
					}
					generator.close();
					return out.toByteArray();
				}

			}
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				try {
					st.setObject( index, toOsonStream( value, getJavaType(), options ), OracleType.JSON );
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				try {
					st.setObject( name, toOsonStream( value, getJavaType(), options ) , OracleType.JSON);
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {

		return new BasicExtractor<>( javaType, this ) {

			private X fromOson(InputStream osonBytes, WrapperOptions options) throws Exception {
				FormatMapper mapper = options.getJsonFormatMapper();
				JsonFactory osonFactory = (JsonFactory) osonFactoryKlass.getDeclaredConstructor().newInstance();
				JsonParser osonParser = osonFactory.createParser( osonBytes );
				return mapper.readFromSource(  getJavaType(), osonParser, options);
			}

			private X doExtraction(OracleJsonDatum datum,  WrapperOptions options) throws SQLException {
				if ( datum == null ) {
					return null;
				}
				InputStream osonBytes = datum.getStream();
				try {
					return fromOson( osonBytes ,options);
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {

				OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
				return doExtraction( ojd, options);
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {


				OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
				return doExtraction( ojd, options);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {

				OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
				return doExtraction( ojd, options);
			}
		};
	}
}
