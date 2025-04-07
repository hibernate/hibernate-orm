/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.type.format.JsonDocumentItemType;
import org.hibernate.type.format.StringJsonDocumentReader;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Jannetti
 */
public class StringJsonDocumentReaderTest {
	@Test
	public void testNullDocument() {
		assertThrows( IllegalArgumentException.class, () -> {new StringJsonDocumentReader( null );} );
	}
	@Test
	public void testEmptyDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "" );
		assertFalse(reader.hasNext(), "Should not have  anymore element");
	}
	@Test
	public void testEmptyJsonObject() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());
		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
	}
	@Test
	public void testEmptyJsonArray() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "[]" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.ARRAY_START, reader.next());
		assertEquals( JsonDocumentItemType.ARRAY_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
	}
	@Test
	public void testWrongNext() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());
		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}
	@Test
	public void testSimpleDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{ \"key1\" :\"value1\"    }" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals("key1", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE, reader.next());
		assertEquals("value1", reader.getStringValue());


		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}
	@Test
	public void testSimpleDoubleValueDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{ \"key1\":\"\",\"key2\" : \" x value2 x \" }" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "key1", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals( "", reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "key2", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertTrue( reader.getStringValue().equals(" x value2 x "));

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}
	@Test
	public void testNonStringValueDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{ \"aNull\":null, \"aNumber\" : 12 , \"aBoolean\" : true}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());


		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aNull", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NULL_VALUE,reader.next());
		assertEquals( "null", reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aNumber", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals( 12, reader.getIntegerValue());
		assertEquals( "12", reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aBoolean", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.BOOLEAN_VALUE,reader.next());
		assertEquals( true, reader.getBooleanValue());
		assertEquals( "true", reader.getStringValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}

	@Test
	public void testNonAvailableValueDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertThrows( IllegalStateException.class, () -> {reader.getStringValue();} );

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}

	@Test
	public void testBooleanValueDocument() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( "{ \"aBoolean\" : true}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aBoolean", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.BOOLEAN_VALUE,reader.next());
		assertTrue( reader.getBooleanValue() );
		assertEquals( "true",reader.getStringValue());
		assertTrue(reader.getBooleanValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}

	@Test
	public void testNumericValueDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( "{ \"aInteger\" : 12, \"aDouble\" : 123.456 , \"aLong\" : 123456, \"aShort\" : 1}" );
		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());


		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aInteger", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals( (int)12 , reader.getIntegerValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aDouble", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals( (double)123.456 ,reader.getDoubleValue()  );

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aLong", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals( (long)123456 , reader.getLongValue()  );

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "aShort", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals( (short)1, reader.getLongValue()  );


		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}

	@Test
	public void testArrayValueDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( "{ \"anEmptyArray\" : [], \"anArray\" : [1,2,3] }" );
		assertTrue(reader.hasNext());
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "anEmptyArray", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.ARRAY_START,reader.next());
		assertEquals( JsonDocumentItemType.ARRAY_END,reader.next());

		assertEquals(  JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "anArray", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.ARRAY_START,reader.next());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals(1,  reader.getIntegerValue());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals(2,  reader.getIntegerValue());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals(3,  reader.getIntegerValue());
		assertEquals( JsonDocumentItemType.ARRAY_END,reader.next());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}
	@Test
	public void testObjectArrayMultipleValueDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( "{ \"anArray\" : [1, null, \"2\" ,  {\"foo\":\"bar\"}  ]  \n"
											+ "     }" );
		assertTrue(reader.hasNext());
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "anArray", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.ARRAY_START,reader.next());

		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals("1",  reader.getStringValue());
		assertEquals( JsonDocumentItemType.NULL_VALUE,reader.next());
		assertEquals("null",  reader.getStringValue());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals(2,  reader.getIntegerValue());

		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());
		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "foo", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("bar",  reader.getStringValue());
		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

		assertEquals( JsonDocumentItemType.ARRAY_END,reader.next());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}
	@Test
	public void testEscapeStringDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( "{ \"str1\" : \"abc\" , \"str2\" : \"\\\"abc\\\"\" , \"str3\" : \"a\\\"b\\\"c\" }" );
		assertTrue(reader.hasNext());
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "str1", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("abc",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "str2", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("\"abc\"",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "str3", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("a\"b\"c",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

		assertFalse(reader.hasNext(), "Should not have  anymore element");
		assertThrows( NoSuchElementException.class, () -> {reader.next();} );
	}

	@Test
	public void testNestedDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( """
							{
							"nested": {
								"converted_gender": "M",
								"theInteger": -1
							},
							"doubleNested": {
								"theNested": {
									"theLeaf": {
										"stringField": "String \\"<abc>A&B</abc>\\""
									}
								}
							},
							"integerField": 10
							}
						""");

		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "nested", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.OBJECT_START,reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "converted_gender", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("M",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "theInteger", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals(-1,  reader.getIntegerValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "doubleNested", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.OBJECT_START,reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "theNested", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.OBJECT_START,reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "theLeaf", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.OBJECT_START,reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "stringField", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE,reader.next());
		assertEquals("String \"<abc>A&B</abc>\"",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "integerField", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals("10",  reader.getStringValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
	}

	@Test
	public void testNestedArrayDocument() {
		final StringJsonDocumentReader reader =
				new StringJsonDocumentReader( """
							{
							"nested": [
										{
											"anArray": [1]
										}
									]
							}
						""");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "nested", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.ARRAY_START,reader.next());

		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals( "anArray", reader.getObjectKeyName());

		assertEquals( JsonDocumentItemType.ARRAY_START,reader.next());
		assertEquals( JsonDocumentItemType.NUMERIC_VALUE,reader.next());
		assertEquals(1L,  reader.getLongValue());
		assertEquals( JsonDocumentItemType.ARRAY_END,reader.next());

		assertEquals( JsonDocumentItemType.OBJECT_END,reader.next());

		assertEquals( JsonDocumentItemType.ARRAY_END,reader.next());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());
	}

	@Test
	public void testUnicode() {
		final StringJsonDocumentReader reader = new StringJsonDocumentReader( """
							{
							"myUnicode1": "\\u0074\\u0068\\u0069\\u0073\\u005f\\u0069\\u0073\\u005f\\u0075\\u006e\\u0069\\u0063\\u006f\\u0064\\u0065",
							"myUnicode2": "this_\\u0069\\u0073_unicode",
							"myUnicode3": "this_is_unicode"
							}


						""");

		assertTrue(reader.hasNext(), "should have more element");
		assertEquals( JsonDocumentItemType.OBJECT_START, reader.next());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals("myUnicode1", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE, reader.next());
		assertEquals("this_is_unicode", reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals("myUnicode2", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE, reader.next());
		assertEquals("this_is_unicode", reader.getStringValue());

		assertEquals( JsonDocumentItemType.VALUE_KEY,reader.next());
		assertEquals("myUnicode3", reader.getObjectKeyName());
		assertEquals( JsonDocumentItemType.VALUE, reader.next());
		assertEquals("this_is_unicode", reader.getStringValue());

		assertEquals( JsonDocumentItemType.OBJECT_END, reader.next());

	}


}
