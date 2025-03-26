/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.dialect.JsonHelper;
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
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		writer.startObject();
		writer.endObject();
		assertEquals( "{}" , writer.toString());
	}

	@Test
	public void testEmptyArray() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		writer.startArray();
		writer.endArray();
		assertEquals( "[]" , writer.toString() );
	}
	@Test
	public void testArray() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		writer.startArray();
		writer.numberValue( Integer.valueOf( 1 ) );
		writer.numberValue( Integer.valueOf( 2 ) );
		writer.numberValue( Integer.valueOf( 3 ) );
		writer.endArray();
		assertEquals( "[1,2,3]" , writer.toString() );
	}

	@Test
	public void testMixedArrayDocument() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		writer.startArray();
		writer.numberValue( Integer.valueOf( 1 ) );
		writer.nullValue();
		writer.booleanValue( false );
		writer.stringValue( "foo" );
		writer.endArray();
		assertEqualsIgnoreSpace( """
									[1,null,false,"foo"]
							""" , writer.toString() );
	}
	@Test
	public void testSimpleDocument() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
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
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter(new JsonHelper.JsonAppender(sb) );
		writer.startObject();
		writer.objectKey( "aNull" );
		writer.nullValue();
		writer.objectKey( "aNumber" );
		writer.numberValue( Integer.valueOf( 12 ) );
		writer.objectKey( "aBoolean" );
		writer.booleanValue( true );
		writer.endObject();

		assertEqualsIgnoreSpace( """
						{
						"aNull":null,
						"aNumber" : 12 ,
							"aBoolean" : true
						}
						""" , writer.toString() );

	}


	@Test
	public void testArrayValueDocument() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter( new JsonHelper.JsonAppender( sb ) );
		writer.startObject();
		writer.objectKey( "anEmptyArray" );
		writer.startArray();
		writer.endArray();
		writer.objectKey( "anArray" );
		writer.startArray();
		writer.numberValue( Integer.valueOf( 1 ) );
		writer.numberValue( Integer.valueOf( 2 ) );
		writer.numberValue( Integer.valueOf( 3 ) );
		writer.endArray();
		writer.endObject();

		assertEqualsIgnoreSpace(  """
				{
				"anEmptyArray" : [],
				"anArray" : [1,2,3]
				}
				""", writer.toString() );
	}
	@Test
	public void testObjectArrayMultipleValueDocument() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter( new JsonHelper.JsonAppender( sb ) );
		writer.startObject();
		writer.objectKey( "anArray" ).startArray().numberValue( Integer.valueOf( 1 ) ).nullValue().stringValue( "2" ).startObject()
		.objectKey( "foo" ).stringValue( "bar" ).endObject().endArray().endObject();

		assertEqualsIgnoreSpace( """
					{
						"anArray" : [1, null, "2" , {\"foo\":\"bar\"}  ]
					}
					""" , sb.toString() );

	}

	@Test
	public void testNestedDocument() {
		StringBuilder sb = new StringBuilder();
		StringJsonDocumentWriter writer = new StringJsonDocumentWriter( new JsonHelper.JsonAppender( sb ) );
		writer.startObject().objectKey( "nested" ).startObject()
				.objectKey( "converted_gender" ).stringValue( "M" ).objectKey( "theInteger" ).numberValue( Integer.valueOf( -1 ) ).endObject()
				.objectKey( "doubleNested" ).startObject()
				.objectKey( "theNested" ).startObject()
				.objectKey( "theLeaf" )
				.startObject().objectKey( "stringField" ).stringValue( "String \"<abc>A&B</abc>\"" ).endObject()
				.endObject()
				.endObject()
				.objectKey( "integerField" ).numberValue( Integer.valueOf( 10 ) )
				.endObject();

		assertEqualsIgnoreSpace( """
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
						""",writer.toString());


	}


}
