/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import oracle.sql.json.OracleJsonParser;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;

/**
 * OSON-based implementation of <code>JsonDocumentReader</code>
 */
public class OsonDocumentReader implements JsonDocumentReader {

	final private OracleJsonParser parser;
	private String currentKeyName;
	private Object currentValue;
	private boolean currentValueIsAString; // avoid later introspection
	/**
	 * Creates a new <code>OsonDocumentReader</code>  on top of a <code>OracleJsonParser</code>
	 * @param parser the parser
	 */
	public OsonDocumentReader(OracleJsonParser parser) {
		this.parser = parser;
	}

	@Override
	public boolean hasNext() {
		return this.parser.hasNext();
	}

	@Override
	public JsonDocumentItem.JsonDocumentItemType next() {
		if (!this.parser.hasNext())
			throw new NoSuchElementException("No more item in JSON document");
		OracleJsonParser.Event evt = this.parser.next();
		currentKeyName = null;
		currentValue = null;
		currentValueIsAString = false;
		switch (evt) {
			case OracleJsonParser.Event.START_OBJECT:
				return JsonDocumentItem.JsonDocumentItemType.OBJECT_START;
			case OracleJsonParser.Event.END_OBJECT:
				return JsonDocumentItem.JsonDocumentItemType.OBJECT_END;
			case OracleJsonParser.Event.START_ARRAY:
				return JsonDocumentItem.JsonDocumentItemType.ARRAY_START;
			case OracleJsonParser.Event.END_ARRAY:
				return JsonDocumentItem.JsonDocumentItemType.ARRAY_END;
			case OracleJsonParser.Event.KEY_NAME:
				currentKeyName = this.parser.getString();
				return JsonDocumentItem.JsonDocumentItemType.VALUE_KEY;
			case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
				currentValue = this.parser.getOffsetDateTime();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_DATE:
			case OracleJsonParser.Event.VALUE_TIMESTAMP:
				currentValue = this.parser.getLocalDateTime();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_INTERVALDS:
				currentValue = this.parser.getDuration();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_INTERVALYM:
				currentValue = this.parser.getPeriod();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_STRING:
				currentValue = this.parser.getString();
				currentValueIsAString = true;
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_TRUE:
				currentValue = Boolean.TRUE;
				return JsonDocumentItem.JsonDocumentItemType.BOOLEAN_VALUE;
			case OracleJsonParser.Event.VALUE_FALSE:
				currentValue = Boolean.FALSE;
				return JsonDocumentItem.JsonDocumentItemType.BOOLEAN_VALUE;
			case OracleJsonParser.Event.VALUE_NULL:
				currentValue = null;
				return JsonDocumentItem.JsonDocumentItemType.NULL_VALUE;
			case OracleJsonParser.Event.VALUE_DECIMAL:
				currentValue = this.parser.getBigDecimal();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_DOUBLE:
				currentValue = this.parser.getDouble();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_FLOAT:
				currentValue = this.parser.getFloat();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			case OracleJsonParser.Event.VALUE_BINARY:
				currentValue = this.parser.getBytes();
				return JsonDocumentItem.JsonDocumentItemType.VALUE;
			default :
				assert false:"Unknown OSON event";
		}
		return null;
	}

	@Override
	public String getObjectKeyName() {
		if (currentKeyName == null)
			throw new IllegalStateException("no object key available");
		return currentKeyName;
	}

	@Override
	public String getStringValue() {
		return (String)currentValue;
	}

	@Override
	public BigDecimal getBigDecimalValue() {
		return (BigDecimal)currentValue;
	}

	@Override
	public BigInteger getBigIntegerValue() {
		return ((BigDecimal)currentValue).toBigInteger();
	}

	@Override
	public double getDoubleValue() {
		if (currentValueIsAString) return Double.parseDouble( (String)currentValue );
		return ((Double)currentValue).doubleValue();
	}

	@Override
	public float getFloatValue() {
		if (currentValueIsAString) return Float.parseFloat( (String)currentValue );
		return ((Float)currentValue).floatValue();
	}

	@Override
	public long getLongValue() {
		if (currentValueIsAString) return Long.parseLong( (String)currentValue );
		return ((BigDecimal)currentValue).longValue();
	}

	@Override
	public int getIntegerValue() {
		if (currentValueIsAString) return Integer.parseInt( (String)currentValue );
		return ((BigDecimal)currentValue).intValue();
	}

	@Override
	public short getShortValue() {
		if (currentValueIsAString) return Short.parseShort( (String)currentValue );
		return  ((BigDecimal)currentValue).shortValue();
	}

	@Override
	public byte getByteValue() {
		if (currentValueIsAString) return Byte.parseByte( (String)currentValue );
		return ((Byte)currentValue).byteValue();
	}

	@Override
	public boolean getBooleanValue() {
		if (currentValueIsAString) return BooleanJavaType.INSTANCE.fromEncodedString((String)currentValue);
		return ((Boolean)currentValue).booleanValue();
	}



	@Override
	public <T> T getValue(JavaType<T> javaType, WrapperOptions options) {
		if ( currentValueIsAString ) {
			if (javaType.equals(PrimitiveByteArrayJavaType.INSTANCE)) {
				// be sure that we have only allowed characters.
				// that may happen for string representation of UUID (i.e 53886a8a-7082-4879-b430-25cb94415be8) for instance
				return javaType.fromEncodedString(  (((String) currentValue).replaceAll( "-","" )) );
			}
			return javaType.fromEncodedString( (String) currentValue );
		}

		Object theOneToBeUsed =  currentValue;
		// handle special cases for Date things
		if ( currentValue.getClass() == LocalDateTime.class ) {
			if ( java.sql.Date.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = Date.valueOf( ((LocalDateTime)currentValue).toLocalDate() );
			}
			else if ( java.time.LocalDate.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = ((LocalDateTime)currentValue).toLocalDate();
			}
			else if ( java.time.LocalTime.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = ((LocalDateTime)currentValue).toLocalTime();
			}
			else if ( java.sql.Time.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = Time.valueOf( ((LocalDateTime)currentValue).toLocalTime() );
			}
			else if ( java.sql.Timestamp.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = Timestamp.valueOf( ((LocalDateTime)currentValue) );
			}
			else if ( java.time.LocalTime.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				theOneToBeUsed = ((LocalDateTime)currentValue).toLocalTime();
			}
			else if ( java.util.Date.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				// better way?
				theOneToBeUsed = java.util.Date.from( ((LocalDateTime)currentValue).atZone( ZoneId.of( "UTC" ) ).toInstant() );
			}
		}

		return javaType.wrap( theOneToBeUsed ,options );

	}
}
