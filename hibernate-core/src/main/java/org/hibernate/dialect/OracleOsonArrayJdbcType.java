/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import oracle.jdbc.OracleType;
import oracle.jdbc.driver.DatabaseError;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonGenerator;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.format.OsonDocumentReader;
import org.hibernate.type.format.OsonDocumentWriter;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hibernate.dialect.OracleOsonJdbcType.OSON_JSON_FACTORY;

/**
 *
 * Type mapping of (JSON) array of JSON SQL data type for Oracle database.
 * This implementation is used when Jackson mapper is used and that the JDBC OSON extension
 * is available.
 *
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class OracleOsonArrayJdbcType extends OracleJsonArrayJdbcType {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OracleOsonArrayJdbcType.class );

	public OracleOsonArrayJdbcType(JdbcType elementJdbcType) {
		super(elementJdbcType);
	}

	@Override
	public String toString() {
		return "OracleOsonArrayJdbcType";
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {

		return new BasicBinder<>( javaType, this ) {

			private <T> byte[] toOsonStream(T value, JavaType<T> javaType, WrapperOptions options) throws Exception {
				final Object[] domainObjects = javaType.unwrap( value, Object[].class, options );
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				try (OracleJsonGenerator generator = OSON_JSON_FACTORY.createJsonBinaryGenerator( out )) {
					final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) javaType).getElementJavaType();
					if ( elementJavaType instanceof UnknownBasicJavaType<?> ) {
						try (Closeable osonGen = OracleOsonJacksonHelper.createWriteTarget( out )) {
							options.getJsonFormatMapper().writeToTarget( value, javaType, osonGen, options );
						}
					}
					else {
						final OsonDocumentWriter writer = new OsonDocumentWriter( generator );
						if ( getElementJdbcType() instanceof JsonJdbcType jsonElementJdbcType ) {
							final EmbeddableMappingType embeddableMappingType = jsonElementJdbcType.getEmbeddableMappingType();
							JsonHelper.serializeArray( embeddableMappingType, domainObjects, options, writer );
						}
						else {
							assert !(getElementJdbcType() instanceof AggregateJdbcType);
							JsonHelper.serializeArray(
									elementJavaType,
									getElementJdbcType(),
									domainObjects,
									options,
									writer
							);
						}
					}
				}
				return out.toByteArray();
			}

			private boolean useUtf8(WrapperOptions options) {
				final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) getJavaType()).getElementJavaType();
				return elementJavaType instanceof UnknownBasicJavaType<?>
					&& !options.getJsonFormatMapper().supportsTargetType( OracleOsonJacksonHelper.WRITER_CLASS );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						final String json = OracleOsonArrayJdbcType.this.toString(
								value,
								getJavaType(),
								options
						);
						st.setBytes( index, json.getBytes( StandardCharsets.UTF_8 ) );
					}
					else {
						st.setObject( index, toOsonStream( value, getJavaType(), options ), OracleType.JSON );
					}
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						final String json = OracleOsonArrayJdbcType.this.toString(
								value,
								getJavaType(),
								options
						);
						st.setBytes( name, json.getBytes( StandardCharsets.UTF_8 ) );
					}
					else {
						st.setObject( name, toOsonStream( value, getJavaType(), options ), OracleType.JSON );
					}
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
				if ( ((BasicPluralJavaType<?>) getJavaType()).getElementJavaType() instanceof UnknownBasicJavaType<?> ) {
					try (Closeable oParser = OracleOsonJacksonHelper.createReadSource( osonBytes )) {
						return options.getJsonFormatMapper().readFromSource( getJavaType(), oParser, options );
					}
				}
				else {
					// embeddable array case.
					return JsonHelper.deserializeArray(
							javaType,
							getElementJdbcType(),
							new OsonDocumentReader( OSON_JSON_FACTORY.createJsonBinaryParser( osonBytes ) ),
							options
					);
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

			private boolean useUtf8(WrapperOptions options) {
				final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) getJavaType()).getElementJavaType();
				return elementJavaType instanceof UnknownBasicJavaType<?>
					&& !options.getJsonFormatMapper().supportsTargetType( OracleOsonJacksonHelper.READER_CLASS );
			}

			private X fromString(byte[] json, WrapperOptions options) throws SQLException {
				if ( json == null ) {
					return null;
				}
				return OracleOsonArrayJdbcType.this.fromString(
						new String( json, StandardCharsets.UTF_8 ),
						getJavaType(),
						options
				);
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						return fromString( rs.getBytes( paramIndex ), options );
					}
					else {
						OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return fromString( rs.getBytes( paramIndex ), options );
					} else {
						throw exc;
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						return fromString( statement.getBytes( index ), options );
					}
					else {
						OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return fromString( statement.getBytes( index ), options );
					} else {
						throw exc;
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						return fromString( statement.getBytes( name ), options );
					}
					else {
						OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return fromString( statement.getBytes( name ), options );
					} else {
						throw exc;
					}
				}
			}
		};
	}
}
