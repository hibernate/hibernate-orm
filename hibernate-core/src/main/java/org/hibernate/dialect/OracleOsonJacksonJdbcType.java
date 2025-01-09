/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleType;
import oracle.sql.json.OracleJsonDatum;
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
import org.hibernate.type.format.jackson.JacksonOsonFormatMapper;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Emmanuel Jannetti
 */
public class OracleOsonJacksonJdbcType extends OracleJsonJdbcType {
	public static final OracleOsonJacksonJdbcType INSTANCE = new OracleOsonJacksonJdbcType( null );
	private static Method jacksonOsonObjectMapperGetter = null;

	static {
		try {
			Class jacksonOsonConverter = OracleOsonJacksonJdbcType.class.getClassLoader().loadClass( "oracle.jdbc.provider.oson.JacksonOsonConverter" );
			jacksonOsonObjectMapperGetter = jacksonOsonConverter.getMethod( "getObjectMapper" );
		}
		catch (ClassNotFoundException | LinkageError | NoSuchMethodException e) {
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

		final ObjectMapper objectMapper;
		try {
			objectMapper = (ObjectMapper) jacksonOsonObjectMapperGetter.invoke( null );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			// should not happen
			throw new RuntimeException("Can't retrieve ObjectMapper from OSON extension", e );
		}

		return new BasicBinder<>( javaType, this ) {

			private <X> byte[] toOson(X value, JavaType<X> javaType, WrapperOptions options) throws Exception {

				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();
				// TODO : we should not have to do this.
				//    for now we have to inject the objectMapper.
				//    As this is not a validated architectural decision, we do not
				//     modify the interface yet.
				((JacksonOsonFormatMapper)mapper).setJacksonObjectMapper(objectMapper, getEmbeddableMappingType());

				if(getEmbeddableMappingType()!= null) {
					return ((JacksonOsonFormatMapper)mapper).toOson(value,javaType,options,getEmbeddableMappingType());
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JsonGenerator osonGen = objectMapper.getFactory().createGenerator( out );
				mapper.writeToTarget( value, javaType, osonGen, options );
				osonGen.close(); // until now
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

		final ObjectMapper objectMapper;
		try {
			objectMapper = (ObjectMapper) jacksonOsonObjectMapperGetter.invoke( null );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			// should not happen
			throw new RuntimeException("Can't retrieve ObjectMapper from OSON extension", e );
		}

		return new BasicExtractor<>( javaType, this ) {

			private X fromOson(byte[] osonBytes, WrapperOptions options) throws Exception {

				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();
				// TODO : we should not have to do this.
				//    for now we have to inject the objectMapper.
				//    As this is not a validated architectural decision, we do not
				//     modify the interface yet.
				((JacksonOsonFormatMapper)mapper).setJacksonObjectMapper(objectMapper, getEmbeddableMappingType() );

				if (getEmbeddableMappingType() != null &&
						getJavaType().getJavaTypeClass() == Object[].class) {
					// We are dealing with embeddable (@Embeddable) and we request
					// an array of objects. We use JsonParser to fetch values
					// and build the array.(as opposed to let Jackson do it as we do not
					// have a proper object definition at that stage).
					return ((JacksonOsonFormatMapper)mapper).toObjectArray(
							getEmbeddableMappingType(), osonBytes, options );
				}

				JavaType <X> type = getJavaType();
				if (getEmbeddableMappingType() != null) {
					// We are dealing with embeddable (@Embeddable)
					type = (JavaType<X>) getEmbeddableMappingType().getJavaType();
				}
				return mapper.readFromSource( type, osonBytes, options );
			}

			private X doExtraction(OracleJsonDatum datum,  WrapperOptions options) throws SQLException {
				if ( datum == null ) {
					return null;
				}
				byte[] osonBytes = datum.shareBytes();
				try {
					return fromOson( osonBytes ,options);
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				// can I use rs.getBinaryStream( paramIndex); ?
				OracleJsonDatum ojd = rs.getObject( paramIndex, OracleJsonDatum.class );
				return doExtraction(ojd,options);
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				// can I use rs.getBinaryStream( paramIndex); ?
				OracleJsonDatum ojd = statement.getObject( index, OracleJsonDatum.class );
				return doExtraction(ojd,options);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				// can I use rs.getBinaryStream( paramIndex); ?
				OracleJsonDatum ojd = statement.getObject( name, OracleJsonDatum.class );
				return doExtraction(ojd,options);
			}

		};
	}


}
