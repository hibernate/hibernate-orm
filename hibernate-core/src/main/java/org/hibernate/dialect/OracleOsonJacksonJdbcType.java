/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import oracle.jdbc.OracleType;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
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
 * Type mapping JSON SQL data type for Oracle database.
 * This implementation is used when Jackson mapper is used and that the JDBC OSON extension
 * is available.
 *
 * @author Emmanuel Jannetti
 */
public class OracleOsonJacksonJdbcType extends OracleJsonJdbcType {
	public static final OracleOsonJacksonJdbcType INSTANCE = new OracleOsonJacksonJdbcType( null );

	private static final Class osonFactoryKlass;
	static {
		try {
			osonFactoryKlass = JacksonOsonFormatMapper.class.getClassLoader().loadClass( "oracle.jdbc.provider.oson.OsonFactory" );
		}
		catch (ClassNotFoundException | LinkageError e) {
			// should not happen as OracleOsonJacksonJdbcType is loaded
			// only when Oracle OSON JDBC extension is present
			// see OracleDialect class.
			throw new ExceptionInInitializerError( "OracleOsonJacksonJdbcType class loaded without OSON extension: " + e.getClass()+" "+ e.getMessage());
		}
	}


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

				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();

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
				JsonFactory osonFactory = (JsonFactory) osonFactoryKlass.getDeclaredConstructor().newInstance();
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

				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();

				if (getEmbeddableMappingType() != null &&
						getJavaType().getJavaTypeClass() == Object[].class) {
					// We are dealing with embeddable (@Embeddable) and we request
					// an array of objects. We use JsonParser to fetch values
					// and build the array.(as opposed to let Jackson do it as we do not
					// have a proper object definition at that stage).
					OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( osonBytes );


					//ObjectArrayOsonDocumentHandler handler = new ObjectArrayOsonDocumentHandler( getEmbeddableMappingType(),
						//	options);
					//OsonHelper.consumeOsonTokens(osonParser, osonParser.next(), handler);

					//osonBytes.reset();
					//OracleJsonParser osonParser2 = new OracleJsonFactory().createJsonBinaryParser( osonBytes);
					Object[] objects =  JsonHelper.deserialize(
							getEmbeddableMappingType(),
							osonParser,
							javaType.getJavaTypeClass() != Object[].class,
							options
					);
					//Object[] objects2 = (Object[]) handler.getObjectArray();
					return (X) objects;
				}

				JavaType <X> type = getJavaType();
				if (getEmbeddableMappingType() != null) {
					// We are dealing with embeddable (@Embeddable)
					type = (JavaType<X>) getEmbeddableMappingType().getJavaType();
				}

				JsonFactory osonFactory = (JsonFactory) osonFactoryKlass.getDeclaredConstructor().newInstance();
				try (JsonParser osonParser = osonFactory.createParser(  osonBytes )) {
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
				 OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
				 return doExtraction(ojd,options);

			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
				return doExtraction(ojd,options);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
				return doExtraction(ojd,options);
			}

		};
	}


}
