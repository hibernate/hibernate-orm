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
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Vector;

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

	public static void assertClassGenerated(String className) {
		try {
			assertNotNull( Class.forName( className ) );
		}
		catch ( ClassNotFoundException e ) {
			fail( e.getMessage() );
		}
	}

	public static void assertClassNotFound(String className) {
		try {
			Class.forName( className );
			fail( "Class " + className + " should not have been found." );
		}
		catch ( ClassNotFoundException e ) {
			// success
		}
	}

	public static void assertNoGeneratedSourceFile(String className) {
		// generate the file name
		String fileName = className.replace( ".", PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		File sourceFile = new File(outBaseDir + PATH_SEPARATOR + fileName);
		assertFalse(sourceFile.exists(), "There should be no source file: " + fileName);

	}

	public static void assertAbsenceOfField(String className, String fieldName) throws ClassNotFoundException {
		assertAbsenceOfField( className, fieldName, "field should not be persistent" );
	}

	public static void assertAbsenceOfField(String className, String fieldName, String errorString)
			throws ClassNotFoundException {
		Assert.assertFalse( isFieldHere( className, fieldName ), errorString );
	}

	public static void assertPresenceOfField(String className, String fieldName, String errorString)
			throws ClassNotFoundException {
		Assert.assertTrue( isFieldHere( className, fieldName ), errorString );
	}

	public static void assertFieldType(String className, String fieldName, Class expectedType, String errorString)
			throws ClassNotFoundException {
		Field field = getField( className, fieldName );
		assertNotNull( field );
		ParameterizedType type = ( ParameterizedType ) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		if ( expectedType.isArray() ) {
			expectedType = expectedType.getComponentType();
			actualType = ( ( GenericArrayType ) actualType ).getGenericComponentType();
		}
		assertEquals( actualType, expectedType, errorString );
	}

	public static void assertSuperClass(String className, String superClassName) {
		Class<?> clazz;
		Class<?> superClazz;
		try {
			clazz = Class.forName( className );
			superClazz = Class.forName( superClassName );
			Assert.assertEquals(
					clazz.getSuperclass(), superClazz,
					"Entity " + superClassName + " should be the superclass of " + className
			);
		}
		catch ( ClassNotFoundException e ) {
			fail( "Unable to load metamodel class: " + e.getMessage() );
		}
	}

	private static boolean isFieldHere(String className, String fieldName) throws ClassNotFoundException {
		return getField( className, fieldName ) != null;
	}

	private static Field getField(String className, String fieldName) throws ClassNotFoundException {
		Class<?> clazz = Class.forName( className );
		try {
			return clazz.getField( fieldName );
		}
		catch ( NoSuchFieldException e ) {
			return null;
		}
	}
}


