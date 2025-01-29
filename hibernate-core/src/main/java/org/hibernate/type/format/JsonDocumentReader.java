/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

/**
 * JSON document reader.
 * Reads a JSON document (i.e., String or OSON bytes) and produce Json item type event.
 * Calling #next() will return one of a JsonDocumentItem.JsonDocumentItemType.
 * The sequence of return types follows JSON specification.
 * <p>
 * When {@link JsonDocumentItemType.VALUE_KEY} is returned #getObjectKeyName() should be called to get the key name.
 * <p>
 * When {@link JsonDocumentItemType.VALUE}, {@link JsonDocumentItemType.BOOLEAN_VALUE}, {@link JsonDocumentItemType.NULL_VALUE} or {@link JsonDocumentItemType.NUMERIC_VALUE} is returned one of the getxxxValue() should be called to get the value.
 * <p>
 *  example :
 *  <pre>
 *	{
 * 	  "key1": "value1",
 * 	  "key2": ["x","y","z"],
 * 	  "key3": {
 *             "key4" : ["a"],
 *             "key5" : {}
 *            },
 *    "key6":12,
 *    "key7":null
 *  }
 *  </pre>
 *  This Json object could be read as follows
 *  <pre>
 *      while (reader.hasNext()) {}
 *         JsonDocumentItemType type = reader.next();
 *         switch(type) {
 *         	   case VALUE_KEY:
 *         	       String keyName = reader.getObjectKeyName();
 *         	       break;
 *         	   case VALUE:
 *         	       String value = reader.getStringValue()
 *         	       break
 *         	    //...
 *         }
 *      }
 *  </pre>
 *  This Json object above would trigger this sequence of events
 *  <pre>
 *    JsonDocumentItemType.OBJECT_START
 *    JsonDocumentItemType.VALUE_KEY      // "key1"
 *    JsonDocumentItemType.VALUE          // "value1"
 *    JsonDocumentItemType.VALUE_KEY      // "key2"
 *    JsonDocumentItemType.ARRAY_START
 *    JsonDocumentItemType.VALUE          // "x"
 *    JsonDocumentItemType.VALUE          // "y"
 *    JsonDocumentItemType.VALUE          // "z"
 *    JsonDocumentItemType.ARRAY_END
 *    JsonDocumentItemType.VALUE_KEY      // "key3"
 *    JsonDocumentItemType.OBJECT_START
 *    JsonDocumentItemType.VALUE_KEY      // "key4"
 *    JsonDocumentItemType.ARRAY_START
 *    JsonDocumentItemType.VALUE          // "a"
 *    JsonDocumentItemType.ARRAY_END
 *    JsonDocumentItemType.VALUE_KEY       // "key5"
 *    JsonDocumentItemType.OBJECT_START
 *    JsonDocumentItemType.OBJECT_END
 *    JsonDocumentItemType.VALUE_KEY       // "key6"
 *    JsonDocumentItemType.NUMERIC_VALUE
 *    JsonDocumentItemType.VALUE_KEY       // "key7"
 *    JsonDocumentItemType.NULL_VALUE
 *    JsonDocumentItemType.OBJECT_END
 *    JsonDocumentItemType.OBJECT_END
 *  </pre>
 *
 * @author Emmanuel Jannetti
 */
public interface JsonDocumentReader extends Iterator<JsonDocumentItem.JsonDocumentItemType> {
	default void forEachRemaining() {
		throw new UnsupportedOperationException("forEachRemaining");
	}

	/**
	 * Gets the key name once JsonDocumentItemType.VALUE_KEY has been received
	 * @return the name
	 */
	String getObjectKeyName();
	/**
	 * Gets value as String
	 * @return the value.
	 */
	String getStringValue();
	/**
	 * Gets value as BigDecimal
	 * @return the value.
	 */
	BigDecimal getBigDecimalValue();
	/**
	 * Gets value as BigInteger
	 * @return the value.
	 */
	BigInteger getBigIntegerValue();
	/**
	 * Gets value as double
	 * @return the value.
	 */
	double getDoubleValue();
	/**
	 * Gets value as float
	 * @return the value.
	 */
	float getFloatValue();
	/**
	 * Gets value as long
	 * @return the value.
	 */
	long getLongValue();
	/**
	 * Gets value as int
	 * @return the value.
	 */
	int getIntegerValue();
	/**
	 * Gets value as short
	 * @return the value.
	 */
	short getShortValue();
	/**
	 * Gets value as byte
	 * @return the value.
	 */
	byte getByteValue();
	/**
	 * Gets value as boolean
	 * @return the value.
	 */
	boolean getBooleanValue();

	/**
	 * Gets value as JavaType
	 * @return the value.
	 */
	<T> T getValue(JavaType<T> javaType, WrapperOptions options);
}
