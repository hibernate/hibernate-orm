/*
 * SPDX-License-Identifier: Apache-2.0
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
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

/**
 * Implementation of <code>JsonDocumentWriter</code> for OSON document.
 * This implementation will produce an Object Array based on
 * embeddable mapping
 * Once All JSON document is handle the mapped Object array can be retrieved using the
 * <code>getObjectArray()</code> method.
 *
 * @author Emmanuel Jannetti
 */
public class OsonDocumentWriter implements JsonDocumentWriter {


	private final OracleJsonGenerator generator;

	/**
	 * Creates a new OSON document writer
	 * @param generator the JSON generator.
	 */
	public OsonDocumentWriter(OracleJsonGenerator generator) {
		this.generator = generator;
	}


	@Override
	public JsonDocumentWriter startObject() {
		this.generator.writeStartObject();
		return this;
	}


	@Override
	public JsonDocumentWriter endObject() {
		this.generator.writeEnd();
		return this;
	}


	@Override
	public JsonDocumentWriter startArray() {
		generator.writeStartArray();
		return this;
	}


	@Override
	public JsonDocumentWriter endArray() {
		generator.writeEnd();
		return this;
	}


	@Override
	public JsonDocumentWriter objectKey(String key) {
		this.generator.writeKey( key );
		return this;
	}


	@Override
	public JsonDocumentWriter nullValue() {
		this.generator.writeNull();
		return this;
	}


	@Override
	public JsonDocumentWriter booleanValue(boolean value) {
		this.generator.write(value);
		return this;
	}


	@Override
	public JsonDocumentWriter stringValue(String value) {
		this.generator.write(value);
		return this;
	}

	@Override
	public <T> JsonDocumentWriter serializeJsonValue(Object value, JavaType<T> javaType, JdbcType jdbcType, WrapperOptions options) {
		serializeValue(value, javaType, jdbcType, options);
		return this;
	}

	/**
	 * Serializes a value according to its mapping type.
	 * This method serializes the value and writes it into the underlying generator
	 *
	 * @param value the value
	 * @param javaType the Java type of the value
	 * @param jdbcType the JDBC SQL type of the value
	 * @param options the wapping options.
	 */
	private <T> void serializeValue(Object value,
								JavaType<T> javaType,
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
				generator.write( javaType.unwrap( (T)value,Integer.class,options ) );
				break;
			case SqlTypes.BOOLEAN:
				generator.write( javaType.unwrap( (T)value,Boolean.class,options ) );
				break;
			case SqlTypes.BIT:
				generator.write( javaType.unwrap( (T)value,Integer.class,options ) );
				break;
			case SqlTypes.BIGINT:
				generator.write( javaType.unwrap( (T)value, BigInteger.class,options ) );
				break;
			case SqlTypes.FLOAT:
				generator.write( javaType.unwrap( (T)value,Float.class,options ) );
				break;
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				generator.write( javaType.unwrap( (T)value,Double.class,options ) );
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
				generator.write( javaType.toString( (T)value ) );
				break;
			case SqlTypes.DATE:
				DATE dd = new DATE(javaType.unwrap( (T)value,java.sql.Date.class,options ));
				OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
				generator.write(jsonDate);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				generator.write( javaType.toString( (T)value ) );
				break;
			case SqlTypes.TIMESTAMP:
				TIMESTAMP TS = new TIMESTAMP(javaType.unwrap( (T)value, Timestamp.class, options ));
				OracleJsonTimestamp writeTimeStamp = new OracleJsonTimestampImpl(TS.shareBytes());
				generator.write(writeTimeStamp);
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				OffsetDateTime dateTime = javaType.unwrap( (T)value, OffsetDateTime.class, options );
				generator.write( dateTime );
				break;
			case SqlTypes.TIMESTAMP_UTC:
				OffsetDateTime odt = javaType.unwrap( (T)value, OffsetDateTime.class, options );
				generator.write( odt );
				break;
			case SqlTypes.NUMERIC:
			case SqlTypes.DECIMAL:
				BigDecimal bd = javaType.unwrap( (T)value, BigDecimal.class, options );
				generator.write( bd );
				break;

			case SqlTypes.DURATION:
			case SqlTypes.UUID:
				generator.write( javaType.toString( (T)value ) );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// how to handle
				byte[] bytes = javaType.unwrap( (T)value, byte[].class, options );
				generator.write( bytes );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				throw new IllegalStateException( "array case should be treated at upper level" );
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}

	}

}
