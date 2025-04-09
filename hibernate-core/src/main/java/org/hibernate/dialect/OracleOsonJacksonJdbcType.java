/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
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
 * Type mapping JSON SQL data type for Oracle database.
 * This implementation is used when Jackson mapper is used and that the JDBC OSON extension
 * is available.
 *
 * @author Emmanuel Jannetti
 */
public class OracleOsonJacksonJdbcType extends OracleJsonJdbcType {
	public static final OracleOsonJacksonJdbcType INSTANCE = new OracleOsonJacksonJdbcType( null );

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( OracleOsonJacksonJdbcType.class );

	private static final OsonFactory osonFactory = new OsonFactory();

	private OracleOsonJacksonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public String toString() {
		return "OracleOsonJacksonJdbcType";
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new OracleOsonJacksonJdbcType( mappingType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {

		if(javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class) {
			return super.getBinder( javaType );
		}

		return new BasicBinder<>( javaType, this ) {

			private <X> byte[] toOson(X value, JavaType<X> javaType, WrapperOptions options) throws Exception {

				FormatMapper mapper = options.getJsonFormatMapper();

				if (getEmbeddableMappingType() != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					// OracleJsonFactory is used and not OracleOsonFactory as Jackson is not involved here
					try (OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out )) {
						OsonDocumentWriter writer = new OsonDocumentWriter( generator);
						JsonHelper.serialize( getEmbeddableMappingType(), value,options,writer);
					}
					return out.toByteArray();
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try (JsonGenerator osonGen = osonFactory.createGenerator( out )) {
					mapper.writeToTarget( value, javaType, osonGen, options );
				}
				return out.toByteArray();
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				try {
					st.setObject( index, toOson( value, getJavaType(), options ), OracleType.JSON);
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				try {
					st.setObject( name, toOson( value, getJavaType(), options ), OracleType.JSON);
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {

		if(javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class) {
			return super.getExtractor( javaType );
		}

		return new BasicExtractor<>( javaType, this ) {

			private X fromOson(InputStream osonBytes, WrapperOptions options) throws Exception {

				FormatMapper mapper = options.getJsonFormatMapper();

				if (getEmbeddableMappingType() != null &&
						getJavaType().getJavaTypeClass() == Object[].class) {
					// We are dealing with embeddable (@Embeddable) and we request
					// an array of objects. We use JsonParser to fetch values
					// and build the array.(as opposed to let Jackson do it as we do not
					// have a proper object definition at that stage).
					OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( osonBytes );
					Object[] objects =  JsonHelper.deserialize(
							getEmbeddableMappingType(),
							new OsonDocumentReader(osonParser),
							javaType.getJavaTypeClass() != Object[].class,
							options
					);
					return (X) objects;
				}

				JavaType <X> type = getJavaType();
				if (getEmbeddableMappingType() != null) {
					// We are dealing with embeddable (@Embeddable)
					type = (JavaType<X>) getEmbeddableMappingType().getJavaType();
				}

				try (JsonParser osonParser = ((JsonFactory)osonFactory).createParser(  osonBytes )) {
					return mapper.readFromSource( type, osonParser, options );
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
					return doExtraction(ojd,options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonJdbcType.this.fromString(
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
					return doExtraction(ojd,options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonJdbcType.this.fromString(
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
					return doExtraction(ojd,options);
				} catch (SQLException exc) {
					if ( exc.getErrorCode() == DatabaseError.JDBC_ERROR_BASE + DatabaseError.EOJ_INVALID_COLUMN_TYPE) {
						// This may happen if we are fetching data from an existing schema
						// that uses BLOB for JSON column In that case we assume bytes are
						// UTF-8 bytes (i.e not OSON) and we fall back to previous String-based implementation
						LOG.invalidJSONColumnType( OracleType.CLOB.getName(), OracleType.JSON.getName() );
						return OracleOsonJacksonJdbcType.this.fromString(
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
