/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import oracle.jdbc.OracleType;
import oracle.jdbc.driver.DatabaseError;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import org.hibernate.dialect.type.OracleJsonJdbcType;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JsonHelper;
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
import java.sql.SQLFeatureNotSupportedException;

/**
 * Type mapping JSON SQL data type for Oracle database.
 * This implementation is used when the JDBC OSON extension is available.
 *
 * @author Emmanuel Jannetti
 */
public class OracleOsonJdbcType extends OracleJsonJdbcType {
	public static final OracleOsonJdbcType INSTANCE = new OracleOsonJdbcType( null );

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OracleOsonJdbcType.class );

	static final OracleJsonFactory OSON_JSON_FACTORY = new OracleJsonFactory();

	private OracleOsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public String toString() {
		return "OracleOsonJdbcType";
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new OracleOsonJdbcType( mappingType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {

		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return super.getBinder( javaType );
		}

		return new BasicBinder<>( javaType, this ) {

			private <T> byte[] toOson(T value, JavaType<T> javaType, WrapperOptions options) throws Exception {
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				if ( getEmbeddableMappingType() != null ) {
					// OracleJsonFactory is used and not OracleOsonFactory as Jackson is not involved here
					try (OracleJsonGenerator generator = OSON_JSON_FACTORY.createJsonBinaryGenerator( out )) {
						JsonHelper.serialize(
								getEmbeddableMappingType(),
								value,
								options,
								new OsonDocumentWriter( generator )
						);
					}
				}
				else {
					try (Closeable osonGen = OracleOsonJacksonHelper.createWriteTarget( out )) {
						options.getJsonFormatMapper().writeToTarget( value, javaType, osonGen, options );
					}
				}
				return out.toByteArray();
			}

			private boolean useUtf8(WrapperOptions options) {
				return getEmbeddableMappingType() == null
					&& !options.getJsonFormatMapper().supportsTargetType( OracleOsonJacksonHelper.WRITER_CLASS );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				try {
					if ( useUtf8( options ) ) {
						final String json = OracleOsonJdbcType.this.toString(
								value,
								getJavaType(),
								options
						);
						st.setBytes( index, json.getBytes( StandardCharsets.UTF_8 ) );
					}
					else {
						st.setObject( index, toOson( value, getJavaType(), options ), OracleType.JSON );
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
						final String json = OracleOsonJdbcType.this.toString(
								value,
								getJavaType(),
								options
						);
						st.setBytes( name, json.getBytes( StandardCharsets.UTF_8 ) );
					}
					else {
						st.setObject( name, toOson( value, getJavaType(), options ), OracleType.JSON );
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

		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return super.getExtractor( javaType );
		}

		return new BasicExtractor<>( javaType, this ) {

			private X fromOson(InputStream osonBytes, WrapperOptions options) throws Exception {
				if ( getEmbeddableMappingType() != null ) {
					return JsonHelper.deserialize(
							getEmbeddableMappingType(),
							new OsonDocumentReader( OSON_JSON_FACTORY.createJsonBinaryParser( osonBytes ) ),
							javaType.getJavaTypeClass() != Object[].class,
							options
					);
				}
				else {
					try (Closeable osonParser = OracleOsonJacksonHelper.createReadSource( osonBytes )) {
						return options.getJsonFormatMapper().readFromSource( getJavaType(), osonParser, options );
					}
				}
			}

			private boolean useUtf8(WrapperOptions options) {
				return getEmbeddableMappingType() == null
					&& !options.getJsonFormatMapper().supportsSourceType( OracleOsonJacksonHelper.READER_CLASS );
			}

			private X doExtraction(OracleJsonDatum datum, WrapperOptions options) throws SQLException {
				if ( datum == null ) {
					return null;
				}
				InputStream osonBytes = datum.getStream();
				try {
					return fromOson( osonBytes, options );
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			private X fromString(byte[] json, WrapperOptions options) throws SQLException {
				if ( json == null ) {
					return null;
				}
				return OracleOsonJdbcType.this.fromString(
						new String( json, StandardCharsets.UTF_8 ),
						getJavaType(),
						options
				);
			}

			private byte[] getBytesFromResultSetByIndex(ResultSet rs, int index) throws SQLException {
				// This can be a BLOB or a CLOB. getBytes is not supported on CLOB
				// and getString is not supported on BLOB. W have to try both
				try {
					return rs.getBytes( index );
				}
				catch (SQLFeatureNotSupportedException nse) {
					return rs.getString( index ).getBytes();
				}
			}

			private byte[] getBytesFromStatementByIndex(CallableStatement st, int index) throws SQLException {
				// This can be a BLOB or a CLOB. getBytes is not supported on CLOB
				// and getString is not supported on BLOB. W have to try both
				try {
					return st.getBytes( index );
				}
				catch (SQLFeatureNotSupportedException nse) {

					return st.getString( index ).getBytes();
				}
			}

			private byte[] getBytesFromStatementByName(CallableStatement st, String columnName) throws SQLException {
				// This can be a BLOB or a CLOB. getBytes is not supported on CLOB
				// and getString is not supported on BLOB. W have to try both
				try {
					return st.getBytes( columnName );
				}
				catch (SQLFeatureNotSupportedException nse) {
					return st.getString( columnName ).getBytes();
				}
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( useUtf8( options ) ) {
					return fromString( getBytesFromResultSetByIndex( rs, paramIndex ), options );
				}
				else {
					try {
						OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
					catch (SQLException exc) {
						if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE ) {
							// This may happen if we are fetching data from an existing schema
							// that uses BLOB for JSON column. In that case we assume bytes are
							// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
							LOG.invalidJSONColumnType( OracleType.BLOB.getName(), OracleType.JSON.getName() );
							return fromString( getBytesFromResultSetByIndex( rs, paramIndex ), options );
						}
						else {
							throw exc;
						}
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( useUtf8( options ) ) {
					return fromString( getBytesFromStatementByIndex( statement, index ), options );
				}
				else {
					try {
						OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
					catch (SQLException exc) {
						if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE ) {
							// This may happen if we are fetching data from an existing schema
							// that uses BLOB for JSON column In that case we assume bytes are
							// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
							LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
							return fromString( getBytesFromStatementByIndex( statement, index ), options );
						}
						else {
							throw exc;
						}
					}
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				if ( useUtf8( options ) ) {
					return fromString( getBytesFromStatementByName( statement, name ), options );
				}
				else {
					try {
						OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
						return doExtraction( ojd, options );
					}
					catch (SQLException exc) {
						if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE ) {
							// This may happen if we are fetching data from an existing schema
							// that uses BLOB for JSON column In that case we assume bytes are
							// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
							LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
							return fromString( getBytesFromStatementByName( statement, name ), options );
						}
						else {
							throw exc;
						}
					}
				}
			}
		};
	}
}
