/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.type.format.StringJsonDocumentWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Emmanuel Jannetti
 */
public class StringJsonDocumentWriterTest {

	private static void assertEqualsIgnoreSpace(String expected, String actual) {
		assertEquals(expected.replaceAll("\\s", ""), actual.replaceAll("\\s", ""));
	}

	@Test
	public void testEmptyDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject();
		writer.endObject();
		assertEquals( "{}" , writer.toString());
	}

	@Test
	public void testEmptyArray() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startArray();
		writer.endArray();
		assertEquals( "[]" , writer.toString() );
	}
	@Test
	public void testArray() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startArray();
		writer.booleanValue( false );
		writer.booleanValue( true );
		writer.booleanValue( false );
		writer.endArray();
		assertEquals( "[false,true,false]" , writer.toString() );
	}

	@Test
	public void testMixedArrayDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startArray();
		writer.nullValue();
		writer.booleanValue( false );
		writer.stringValue( "foo" );
		writer.endArray();
		assertEqualsIgnoreSpace( """
									[null,false,"foo"]
							""" , writer.toString() );
	}
	@Test
	public void testSimpleDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject();
		writer.objectKey( "key1" );
		writer.stringValue( "value1" );
		writer.endObject();

		assertEqualsIgnoreSpace(
		"""
			{
			"key1":"value1"
			}
			""", writer.toString());

	}

	@Test
	public void testNonStringValueDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject();
		writer.objectKey( "aNull" );
		writer.nullValue();
		writer.objectKey( "aBoolean" );
		writer.booleanValue( true );
		writer.endObject();

		assertEqualsIgnoreSpace( """
						{
						"aNull":null,
						"aBoolean" : true
						}
						""" , writer.toString() );

	}


	@Test
	public void testArrayValueDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject();
		writer.objectKey( "anEmptyArray" );
		writer.startArray();
		writer.endArray();
		writer.objectKey( "anArray" );
		writer.startArray();
		writer.stringValue( "1" );
		writer.stringValue( "2" );
		writer.stringValue( "3" );
		writer.endArray();
		writer.endObject();

		assertEqualsIgnoreSpace(  """
				{
				"anEmptyArray" : [],
				"anArray" : ["1","2","3"]
				}
				""", writer.toString() );
	}
	@Test
	public void testObjectArrayMultipleValueDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject();
		writer.objectKey( "anArray" ).startArray().nullValue().stringValue( "2" ).startObject()
		.objectKey( "foo" ).stringValue( "bar" ).endObject().endArray().endObject();

		assertEqualsIgnoreSpace( """
					{
						"anArray" : [null, "2" , {\"foo\":\"bar\"}  ]
					}
					""" , writer.toString() );

	}

	@Test
	public void testNestedDocument() {
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
		writer.startObject().objectKey( "nested" ).startObject()
				.objectKey( "converted_gender" ).stringValue( "M" )
				.endObject()
				.objectKey( "doubleNested" ).startObject()
				.objectKey( "theNested" ).startObject()
				.objectKey( "theLeaf" )
				.startObject().objectKey( "stringField" ).stringValue( "String \"<abc>A&B</abc>\"" ).endObject()
				.endObject()
				.endObject()
				.endObject();

		assertEqualsIgnoreSpace( """
							{
							"nested": {
								"converted_gender": "M"
							},
							"doubleNested": {
								"theNested": {
									"theLeaf": {
										"stringField": "String \\"<abc>A&B</abc>\\""
									}
								}
							}
							}
						""",writer.toString());


	}


}
