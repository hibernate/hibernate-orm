/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.sql.json.OracleJsonDatum;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.hibernate.type.format.jackson.JacksonOsonFormatMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Emmanuel Jannetti
 */
public class OracleOsonJacksonArrayJdbcType extends OracleJsonArrayJdbcType {

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
			throw new ExceptionInInitializerError( "OracleOsonJacksonArrayJdbcType class loaded without OSON extension: " + e.getClass()+" "+ e.getMessage());
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

		final ObjectMapper objectMapper;
		try {
			objectMapper = (ObjectMapper) jacksonOsonObjectMapperGetter.invoke( null );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			// should not happen
			throw new RuntimeException("Can't retrieve ObjectMapper from OSON extension", e );
		}

		return new BasicBinder<>( javaType, this ) {

			private <X> InputStream toOson(X value, JavaType<X> javaType, WrapperOptions options) throws Exception {
				// TODO : We should rely on
				//       FormatMapper fm = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();
				//
				//     But this do not let use inject our ObjectMapper. For now create our own instance
				FormatMapper mapper = new JacksonJsonFormatMapper(objectMapper);

				ByteArrayOutputStream out = new ByteArrayOutputStream();

				JsonGenerator osonGen = objectMapper.getFactory().createGenerator( out );
				mapper.writeToTarget( value, javaType, osonGen, options );
				osonGen.close();
				ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
				return in;
			}
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				try {
					st.setBinaryStream( index, toOson( value, getJavaType(), options ) );
				}
				catch (Exception e) {
					throw new SQLException( e );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				try {
					st.setBinaryStream( name, toOson( value, getJavaType(), options ) );
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
				// TODO : We should rely on
				//       FormatMapper fm = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();
				//
				//     But this do not let use inject our ObjectMapper. For now create our own instance
				FormatMapper mapper = new JacksonOsonFormatMapper(objectMapper);
				return mapper.readFromSource(  getJavaType(), osonBytes, options);
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
