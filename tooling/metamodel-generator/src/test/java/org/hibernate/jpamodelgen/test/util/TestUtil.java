/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.tools.Diagnostic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.FileAssert.fail;

/**
 * @author Hardy Ferentschik
 */
public class TestUtil {
	private static final Logger log = LoggerFactory.getLogger( TestUtil.class );
	private static final String PATH_SEPARATOR = System.getProperty( "file.separator" );
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String META_MODEL_CLASS_POSTFIX = "_";
	private static final String OUT_BASE_DIR;

	static {
		String tmp = System.getProperty( "outBaseDir" );
		if ( tmp == null ) {
			fail( "The system property outBaseDir has to be set and point to the base directory of the test output directory." );
		}
		OUT_BASE_DIR = tmp;
	}

	private TestUtil() {
	}

	public static void deleteGeneratedSourceFiles(File path) {
		if ( path.exists() ) {
			File[] files = path.listFiles( new MetaModelFilenameFilter() );
			for ( File file : files ) {
				if ( file.isDirectory() ) {
					deleteGeneratedSourceFiles( file );
				}
				else {
					boolean success = file.delete();
					if ( success ) {
						log.debug( file.getAbsolutePath() + " deleted successfully" );
					}
					else {
						log.debug( "Failed to delete generated source file" + file.getAbsolutePath() );
					}
				}
			}
		}
	}

	public static Class<?> getMetamodelClassFor(Class<?> entityClass) {
		String entityModelClassName = entityClass.getName() + META_MODEL_CLASS_POSTFIX;

		try {
			return Class.forName( entityModelClassName );
		}
		catch ( ClassNotFoundException e ) {
			fail( "Unable to load class " + entityModelClassName );
			return null;
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

	public static File getMetaModelSourceFileFor(Class<?> clazz) {
		String metaModelClassName = clazz.getName() + META_MODEL_CLASS_POSTFIX;
		// generate the file name
		String fileName = metaModelClassName.replace( PACKAGE_SEPARATOR, PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		return new File( OUT_BASE_DIR + PATH_SEPARATOR + fileName );
	}

	public static String getMetaModelSourceAsString(Class<?> clazz) {
		File sourceFile = getMetaModelSourceFileFor( clazz );
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader( new FileReader( sourceFile ) );
			try {
				String line = null;
				/*
						* readLine is a bit quirky :
						* it returns the content of a line MINUS the newline.
						* it returns null only for the END of the stream.
						* it returns an empty String if two newlines appear in a row.
						*/
				while ( ( line = input.readLine() ) != null ) {
					contents.append( line );
					contents.append( System.getProperty( "line.separator" ) );
				}
			}
			finally {
				input.close();
			}
		}
		catch ( IOException ex ) {
			ex.printStackTrace();
		}

		return contents.toString();
	}

	public static void dumpMetaModelSourceFor(Class<?> clazz) {
		log.info( "Dumping meta model source for " + clazz.getName() + ":" );
		log.info( getMetaModelSourceAsString( clazz ) );
	}

	public static void assertNoSourceFileGeneratedFor(Class<?> clazz) {
		assertNotNull( clazz, "Class parameter cannot be null" );
		File sourceFile = getMetaModelSourceFileFor( clazz );
		assertFalse( sourceFile.exists(), "There should be no source file: " + sourceFile.getName() );
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		assertAbsenceOfFieldInMetamodelFor(
				clazz,
				fieldName,
				"'" + fieldName + "' should not appear in metamodel class"
		);
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertFalse( hasFieldInMetamodelFor( clazz, fieldName ), buildErrorString( errorString, clazz ) );
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		assertPresenceOfFieldInMetamodelFor(
				clazz,
				fieldName,
				"'" + fieldName + "' should appear in metamodel class"
		);
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertTrue( hasFieldInMetamodelFor( clazz, fieldName ), buildErrorString( errorString, clazz ) );
	}

	public static void assertAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field, "Cannot find field '" + fieldName + "' in " + clazz.getName() );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		if ( expectedType.isArray() ) {
			expectedType = expectedType.getComponentType();
			actualType = getComponentType( actualType );
		}
		assertEquals(
				actualType,
				expectedType,
				"Types do not match: " + buildErrorString( errorString, clazz )
		);
	}

	public static void assertMapAttributesInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedMapKey, Class<?> expectedMapValue, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualMapKeyType = type.getActualTypeArguments()[1];
		assertEquals( actualMapKeyType, expectedMapKey, buildErrorString( errorString, clazz ) );

		Type actualMapKeyValue = type.getActualTypeArguments()[2];
		assertEquals( actualMapKeyValue, expectedMapValue, buildErrorString( errorString, clazz ) );
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

	public static Field getFieldFromMetamodelFor(Class<?> entityClass, String fieldName) {
		Class<?> metaModelClass = getMetamodelClassFor( entityClass );
		Field field;
		try {
			field = metaModelClass.getDeclaredField( fieldName );
		}
		catch ( NoSuchFieldException e ) {
			field = null;
		}
		return field;
	}

	public static String fcnToPath(String fcn) {
		return fcn.replace( PACKAGE_SEPARATOR, PATH_SEPARATOR );
	}

	public static void assertNoCompilationError(List<Diagnostic<?>> diagnostics) {
		for ( Diagnostic<?> diagnostic : diagnostics ) {
			if ( diagnostic.getKind().equals( Diagnostic.Kind.ERROR ) ) {
				fail( "There was a compilation error. " + diagnostic.getMessage( null ) );
			}
		}
	}

	private static boolean hasFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		return getFieldFromMetamodelFor( clazz, fieldName ) != null;
	}

	private static String buildErrorString(String baseError, Class<?> clazz) {
		StringBuilder builder = new StringBuilder();
		builder.append( baseError );
		builder.append( "\n" );
		builder.append( "Source code for " );
		builder.append( clazz.getName() );
		builder.append( "_.java:" );
		builder.append( "\n" );
		builder.append( getMetaModelSourceAsString( clazz ) );
		return builder.toString();
	}

	private static Type getComponentType(Type actualType) {
		if ( actualType instanceof Class ) {
			Class<?> clazz = (Class<?>) actualType;
			if ( clazz.isArray() ) {
				return clazz.getComponentType();
			}
			else {
				fail( "Unexpected component type" );
			}
		}

		if ( actualType instanceof GenericArrayType ) {
			return ( (GenericArrayType) actualType ).getGenericComponentType();
		}
		else {
			fail( "Unexpected component type" );
			return null;
		}
	}

	private static class MetaModelFilenameFilter implements FileFilter {
		@Override
		public boolean accept(File pathName) {
			if ( pathName.isDirectory() ) {
				return true;
			}
			else {
				return pathName.getAbsolutePath().endsWith( "_.java" )
						|| pathName.getAbsolutePath().endsWith( "_.class" );
			}
		}
	}
}


