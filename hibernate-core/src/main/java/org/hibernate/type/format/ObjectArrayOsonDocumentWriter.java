/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import oracle.jdbc.driver.json.tree.OracleJsonDateImpl;
import oracle.jdbc.driver.json.tree.OracleJsonTimestampImpl;
import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import oracle.sql.json.OracleJsonDate;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Implementation of <code>JsonDocumentWriter</code> for OSON document.
 * This implementation will produce an Object Array based on
 * embeddable mapping
 * Once All JSON document is handle the mapped Object array can be retrieved using the
 * <code>getObjectArray()</code> method.
 *
 * @author Emmanuel Jannetti
 */
public class ObjectArrayOsonDocumentWriter implements JsonDocumentWriter {


	private final OracleJsonGenerator generator;

	public ObjectArrayOsonDocumentWriter(OracleJsonGenerator generator) {
		this.generator = generator;
	}


	@Override
	public void startObject() throws IOException {
		this.generator.writeStartObject();
	}


	@Override
	public void endObject() throws IOException {
		this.generator.writeEnd();
	}


	@Override
	public void startArray() {
		generator.writeStartArray();
	}


	@Override
	public void endArray() {
		generator.writeEnd();
	}


	@Override
	public void objectKey(String key) {
		this.generator.writeKey( key );
	}


	@Override
	public void nullValue() {
		this.generator.writeNull();
	}


	@Override
	public void booleanValue(boolean value) {
		this.generator.write(value);
	}


	@Override
	public void stringValue(String value) {
		this.generator.write(value);
	}


	@Override
	public void numberValue(Number value) {
		this.generator.write((BigDecimal) value );
	}

	@Override
	public void serializeJsonValue(Object value, JavaType<Object> javaType, JdbcType jdbcType, WrapperOptions options) {
		serializeValue(value, javaType, jdbcType, options);
	}


	private void serializeValue(Object value,
								JavaType<Object> javaType,
								JdbcType jdbcType,
								WrapperOptions options) {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( value instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					int i = ((Boolean) value) ? 1 : 0;
					generator.write( i );
					break;
				}
				if ( value instanceof Enum ) {
					generator.write( ((Enum<?>) value ).ordinal() );
					break;
				}
				generator.write( javaType.unwrap( value,Integer.class,options ) );
				break;
			case SqlTypes.BOOLEAN:
				generator.write( javaType.unwrap( value,Boolean.class,options ) );
				break;
			case SqlTypes.BIT:
				generator.write( javaType.unwrap( value,Integer.class,options ) );
				break;
			case SqlTypes.BIGINT:
				generator.write( javaType.unwrap( value, BigInteger.class,options ) );
				break;
			case SqlTypes.FLOAT:
				generator.write( javaType.unwrap( value,Float.class,options ) );
				break;
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				generator.write( javaType.unwrap( value,Double.class,options ) );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean ) {
					String c = ((Boolean) value) ? "Y" : "N";
					generator.write( c );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.CLOB:
			case SqlTypes.MATERIALIZED_CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.MATERIALIZED_NCLOB:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				// correct?
				generator.write( javaType.unwrap( value,String.class,options ) );
				break;
			case SqlTypes.DATE:
				DATE dd = new DATE(javaType.unwrap( value,java.sql.Date.class,options ));
				OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
				generator.write(jsonDate);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				Time time = javaType.unwrap( value, Time.class,options );
				generator.write( time.toString() );
				break;
			case SqlTypes.TIMESTAMP:
				TIMESTAMP TS = new TIMESTAMP(javaType.unwrap( value, Timestamp.class, options ));
				OracleJsonTimestamp writeTimeStamp = new OracleJsonTimestampImpl(TS.shareBytes());
				generator.write(writeTimeStamp);
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				try {
					OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, options );
					generator.write( dateTime );
				}
				catch (Exception e) {
					Timestamp tswtz = javaType.unwrap( value, Timestamp.class, options );
					TIMESTAMP TSWTZ = new TIMESTAMP(tswtz);
					OracleJsonTimestamp writeTimeStampWTZ = new OracleJsonTimestampImpl(TSWTZ.shareBytes());
					generator.write(writeTimeStampWTZ);
				}
				break;
			case SqlTypes.TIMESTAMP_UTC:
				if( value instanceof OffsetDateTime ) {
					OffsetDateTime odt = javaType.unwrap( value, OffsetDateTime.class, options );
					generator.write( odt );
					break;
				}
				else if (value instanceof Instant ) {
					Instant instant = javaType.unwrap( value, Instant.class, options );
					generator.write(instant.atOffset( ZoneOffset.UTC )  );
					break;
				}
				generator.write( javaType.unwrap( value,String.class,options ) );
				break;
			case SqlTypes.NUMERIC:
			case SqlTypes.DECIMAL:
				BigDecimal bd = javaType.unwrap( value, BigDecimal.class, options );
				generator.write( bd );
				break;

			case SqlTypes.DURATION:
				Duration duration = javaType.unwrap( value, Duration.class, options );
				generator.write( duration );
				break;
			case SqlTypes.UUID:
				generator.write( UUIDJavaType.INSTANCE.unwrap( (UUID)value, byte[].class, options ) );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// how to handle
				byte[] bytes = javaType.unwrap( value, byte[].class, options );
				generator.write( bytes );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				assert false:"array case should be treated at upper level";
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}

	}

}
