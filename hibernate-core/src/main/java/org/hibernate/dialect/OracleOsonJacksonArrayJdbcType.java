/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import oracle.sql.json.OracleJsonDatum;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonOsonFormatMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Emmanuel Jannetti
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

		return new BasicBinder<>( javaType, this ) {

			private <X> InputStream toOson(X value, JavaType<X> javaType, WrapperOptions options) throws Exception {
				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JsonFactory osonFactory = (JsonFactory) osonFactoryKlass.getDeclaredConstructor().newInstance();
				JsonGenerator osonGen = osonFactory.createGenerator( out );
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

		return new BasicExtractor<>( javaType, this ) {

			private X fromOson(byte[] osonBytes, WrapperOptions options) throws Exception {
				FormatMapper mapper = options.getSession().getSessionFactory().getFastSessionServices().getJsonFormatMapper();

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
