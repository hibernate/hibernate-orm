// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.test.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.testng.Assert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.FileAssert.fail;

/**
 * @author Hardy Ferentschik
 */
public class TestUtil {

	private static final String PATH_SEPARATOR = System.getProperty( "file.separator" );
	private static final String META_MODEL_CLASS_POSTFIX = "_";
	private static final String outBaseDir;

	static {
		String tmp = System.getProperty( "outBaseDir" );
		if ( tmp == null ) {
			fail( "The system property outBaseDir has to be set and point to the base directory of the test output directory." );
		}
		outBaseDir = tmp;
	}

	private TestUtil() {
	}

	public static void clearOutputFolder() {
		File outDir = new File( outBaseDir );
		File[] files = outDir.listFiles();
		for ( File file : files ) {
			file.delete();
		}
	}

	/**
	 * Asserts that a metamodel class for the specified class got generated.
	 *
	 * @param clazz the class for which a metamodel class should have been generated.
	 */
	public static void assertMetamodelClassGeneratedFor(Class<?> clazz) {
		assertNotNull( clazz, "Class parameter cannot be null" );
		String metaModelClassName = clazz.getName() + META_MODEL_CLASS_POSTFIX;
		try {
			assertNotNull( Class.forName( metaModelClassName ) );
		}
		catch ( ClassNotFoundException e ) {
			fail( metaModelClassName + " was not generated." );
		}
	}

	public static void assertNoSourceFileGeneratedFor(Class<?> clazz) {
		assertNotNull( clazz, "Class parameter cannot be null" );
		String metaModelClassName = clazz.getName() + META_MODEL_CLASS_POSTFIX;
		// generate the file name
		String fileName = metaModelClassName.replace( ".", PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		File sourceFile = new File( outBaseDir + PATH_SEPARATOR + fileName );
		assertFalse( sourceFile.exists(), "There should be no source file: " + fileName );
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		assertAbsenceOfFieldInMetamodelFor( clazz, fieldName, "field should not be persistent" );
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		Assert.assertFalse( hasFieldInMetamodelFor( clazz, fieldName ), errorString );
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		Assert.assertTrue( hasFieldInMetamodelFor( clazz, fieldName ), errorString );
	}

	public static void assertFieldTypeInMetaModelFor(Class<?> clazz, String fieldName, Class expectedType, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field );
		ParameterizedType type = ( ParameterizedType ) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		if ( expectedType.isArray() ) {
			expectedType = expectedType.getComponentType();
			actualType = ( ( GenericArrayType ) actualType ).getGenericComponentType();
		}
		assertEquals( actualType, expectedType, errorString );
	}

	public static void assertSuperClassRelationShipInMetamodel(Class<?> entityClass, Class<?> superEntityClass) {
		String entityModelClassName = entityClass.getName() + META_MODEL_CLASS_POSTFIX;
		String superEntityModelClassName = superEntityClass.getName() + META_MODEL_CLASS_POSTFIX;
		Class<?> clazz;
		Class<?> superClazz;
		try {
			clazz = Class.forName( entityModelClassName );
			superClazz = Class.forName( superEntityModelClassName );
			Assert.assertEquals(
					clazz.getSuperclass(), superClazz,
					"Entity " + superEntityModelClassName + " should be the superclass of " + entityModelClassName
			);
		}
		catch ( ClassNotFoundException e ) {
			fail( "Unable to load metamodel class: " + e.getMessage() );
		}
	}

	private static boolean hasFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		return getFieldFromMetamodelFor( clazz, fieldName ) != null;
	}

	public static Field getFieldFromMetamodelFor(Class<?> entityClass, String fieldName) {
		String entityModelClassName = entityClass.getName() + META_MODEL_CLASS_POSTFIX;
		Field field = null;
		try {
			Class<?> clazz = Class.forName( entityModelClassName );
			field = clazz.getField( fieldName );
		}
		catch ( ClassNotFoundException e ) {
			fail( "Unable to load class " + entityModelClassName );
		}
		catch ( NoSuchFieldException e ) {
			field = null;
		}
		return field;
	}
}


