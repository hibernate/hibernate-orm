/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import oracle.jdbc.OracleType;
import oracle.jdbc.driver.DatabaseError;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;

import oracle.sql.json.OracleJsonParser;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
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
import org.hibernate.type.format.OsonDocumentReader;
import org.hibernate.type.format.OsonDocumentWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OracleOsonJacksonArrayJdbcType.class );

	private static final OsonFactory osonFactory = new OsonFactory();

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
				OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( osonBytes );
				final JdbcType elementJdbcType = getElementJdbcType();
				if (elementJdbcType instanceof JsonJdbcType) {
					if (((JsonJdbcType) elementJdbcType).getEmbeddableMappingType() != null) {
						// embeddable array case.
						return JsonHelper.deserializeArray( javaType,
								elementJdbcType, new OsonDocumentReader( osonParser ), options );
					}
				}
				try (JsonParser oParser = ((JsonFactory)osonFactory).createParser(  osonBytes )) {
					return mapper.readFromSource(  getJavaType(), oParser, options);
				}

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
				try {
					OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
					return doExtraction( ojd, options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonArrayJdbcType.this.fromString(
								new String( rs.getBytes( paramIndex ), StandardCharsets.UTF_8 ),
								getJavaType(),
								options);
					} else {
						throw exc;
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				try {
					OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
					return doExtraction( ojd, options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonArrayJdbcType.this.fromString(
								new String( statement.getBytes( index ), StandardCharsets.UTF_8 ),
								getJavaType(),
								options);
					} else {
						throw exc;
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				try {
					OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
					return doExtraction( ojd, options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonArrayJdbcType.this.fromString(
								new String( statement.getBytes( name ), StandardCharsets.UTF_8 ),
								getJavaType(),
								options);
					} else {
						throw exc;
					}
				}
			}
		};
	}
}
