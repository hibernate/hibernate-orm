/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;

import org.jboss.logging.Logger;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SetAttribute;

/**
 * @author Hardy Ferentschik
 */
public class TestUtil {

	private static final Logger log = Logger.getLogger( TestUtil.class );

	private static final String PATH_SEPARATOR = File.separator;
	public static final String RESOURCE_SEPARATOR = "/";
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String META_MODEL_CLASS_POSTFIX = "_";

	private TestUtil() {
	}

	public static void assertNoSourceFileGeneratedFor(Class<?> clazz) {
		assertNotNull( clazz, "Class parameter cannot be null" );
		File sourceFile = getMetaModelSourceFileFor( clazz, false );
		assertFalse( sourceFile.exists(), "There should be no source file: " + sourceFile.getName() );
	}

	public static void assertAbsenceOfNonDefaultConstructorInMetamodelFor(Class<?> clazz, String errorString) {
		assertFalse( hasNonDefaultConstructorInMetamodelFor( clazz ), buildErrorString( errorString, clazz ) );
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

	public static void assertPresenceOfFieldInMetamodelFor(String className, String fieldName) {
		assertPresenceOfFieldInMetamodelFor(
				className,
				fieldName,
				"'" + fieldName + "' should appear in metamodel class"
		);
	}

	public static void assertPresenceOfMethodInMetamodelFor(Class<?> clazz, String methodName, Class<?>... params) {
		assertPresenceOfMethodInMetamodelFor(
				clazz,
				methodName,
				"'" + methodName + "' should appear in metamodel class",
				params
		);
	}

	public static void assertPresenceOfMethodInMetamodelFor(String className, String methodName, Class<?>... params) {
		assertPresenceOfMethodInMetamodelFor(
				className,
				methodName,
				"'" + methodName + "' should appear in metamodel class",
				params
		);
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertTrue( hasFieldInMetamodelFor( clazz, fieldName ), buildErrorString( errorString, clazz ) );
	}

	public static void assertPresenceOfFieldInMetamodelFor(String className, String fieldName, String errorString) {
		assertTrue( hasFieldInMetamodelFor( className, fieldName ), buildErrorString( errorString, className ) );
	}

	public static void assertPresenceOfMethodInMetamodelFor(Class<?> clazz, String fieldName, String errorString,
			Class<?>... params) {
		assertTrue( hasMethodInMetamodelFor( clazz, fieldName, params ), buildErrorString( errorString, clazz ) );
	}

	public static void assertPresenceOfMethodInMetamodelFor(String className, String fieldName, String errorString,
			Class<?>... params) {
		assertTrue( hasMethodInMetamodelFor( className, fieldName, params ), buildErrorString( errorString, className ) );
	}

	public static void assertPresenceOfNameFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertTrue( hasFieldInMetamodelFor( clazz, fieldName ), buildErrorString( errorString, clazz ) );
		assertEquals(
				getFieldFromMetamodelFor( clazz, fieldName ).getType(),
				String.class,
				buildErrorString( errorString, clazz )
		);
	}

	public static void assertAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType,
			String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field, "Cannot find field '" + fieldName + "' in " + clazz.getName() );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		if ( expectedType.isArray() ) {
			expectedType = expectedType.getComponentType();
			actualType = getComponentType( actualType );
		}
		assertEquals(
				expectedType,
				actualType,
				"Types do not match: " + buildErrorString( errorString, clazz )
		);
	}

	public static void assertSetAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType,
			String errorString) {
		assertCollectionAttributeTypeInMetaModelFor( clazz, fieldName, SetAttribute.class, expectedType, errorString );
	}

	public static void assertListAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType,
			String errorString) {
		assertCollectionAttributeTypeInMetaModelFor( clazz, fieldName, ListAttribute.class, expectedType, errorString );
	}

	public static void assertMapAttributesInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedMapKey,
			Class<?> expectedMapValue, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualMapKeyType = type.getActualTypeArguments()[1];
		assertEquals( expectedMapKey, actualMapKeyType, buildErrorString( errorString, clazz ) );

		Type actualMapKeyValue = type.getActualTypeArguments()[2];
		assertEquals( expectedMapValue, actualMapKeyValue, buildErrorString( errorString, clazz ) );
	}

	public static void assertSuperclassRelationshipInMetamodel(Class<?> entityClass, Class<?> superEntityClass) {
		Class<?> clazz = getMetamodelClassFor( entityClass );
		Class<?> superClazz = getMetamodelClassFor( superEntityClass );
		assertEquals(
				superClazz.getName(),
				clazz.getSuperclass().getName(),
				"Entity " + superClazz.getName() + " should be the superclass of " + clazz.getName()
		);
	}

	public static void assertNoCompilationError(List<Diagnostic<?>> diagnostics) {
		for ( Diagnostic<?> diagnostic : diagnostics ) {
			if ( diagnostic.getKind().equals( Diagnostic.Kind.ERROR ) ) {
				fail( "There was a compilation error during annotation processing:\n" + diagnostic );
			}
		}
	}

	/**
	 * Asserts that a metamodel class for the specified class got generated.
	 *
	 * @param clazz the class for which a metamodel class should have been generated.
	 */
	public static void assertMetamodelClassGeneratedFor(Class<?> clazz) {
		assertNotNull( getMetamodelClassFor( clazz ) );
	}

	public static void assertMetamodelClassGeneratedFor(String className) {
		assertNotNull( getMetamodelClassFor( className ) );
	}

	/**
	 * Asserts that a metamodel class for the specified class got generated.
	 *
	 * @param clazz the class for which a metamodel class should have been generated.
	 */
	public static void assertMetamodelClassGeneratedFor(Class<?> clazz, boolean prefix) {
		assertNotNull( getMetamodelClassFor( clazz, prefix ) );
	}

	public static void assertNoMetamodelClassGeneratedFor(Class<?> clazz) {
		try {
			getMetamodelClassFor( clazz );
			fail();
		}
		catch (AssertionError ae) {
		}
	}

	/**
	 * Deletes recursively all files found in the output directory for the annotation processor.
	 * @return the output directory for the generated source and class files.
	 */
	public static void deleteProcessorGeneratedFiles(Class<?> testClass) {
		for ( File file : getOutBaseDir(testClass).listFiles() ) {
			deleteFilesRecursive( file );
		}
	}

	/**
	 * @return the output directory for the generated source and class files.
	 */
	public static File getOutBaseDir(Class<?> testClass) {
		File targetDir = getTargetDir( testClass );
		File outBaseDir = new File( targetDir, "processor-generated-test-classes" );
		if ( !outBaseDir.exists() ) {
			if ( !outBaseDir.mkdirs() ) {
				fail( "Unable to create test output directory " + outBaseDir );
			}
		}
		return outBaseDir;
	}

	public static File getSourceBaseDir(Class<?> testClass) {
		return getBaseDir( testClass, "java" );
	}

	public static File getResourcesBaseDir(Class<?> testClass) {
		return getBaseDir( testClass, "resources" );
	}

	private static File getBaseDir(Class<?> testClass, String type) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		String currentTestClassName = testClass.getName();
		int hopsToCompileDirectory = currentTestClassName.split( "\\." ).length;
		URL classURL = contextClassLoader.getResource( currentTestClassName.replace( '.', '/' ) + ".class" );
		File targetDir = new File( classURL.getFile() );
		// navigate back to '/target'
		for ( int i = 0; i < hopsToCompileDirectory; i++ ) {
			targetDir = targetDir.getParentFile();
		}
		final String configurationDirectory = targetDir.getName();
		final File baseDir = targetDir.getParentFile().getParentFile().getParentFile().getParentFile();
		final File outBaseDir = new File( baseDir, "src/" + configurationDirectory + "/" + type );
		if ( !outBaseDir.exists() ) {
			if ( !outBaseDir.mkdirs() ) {
				fail( "Unable to create test output directory " + outBaseDir );
			}
		}
		return outBaseDir;
	}

	/**
	 * Returns the static metamodel class for the specified entity.
	 *
	 * @param entityClass the entity for which to retrieve the metamodel class. Cannot be {@code null}.
	 *
	 * @return the static metamodel class for the specified entity.
	 */
	public static Class<?> getMetamodelClassFor(Class<?> entityClass) {
		return getMetamodelClassFor( entityClass, false );
	}
	/**
	 * Returns the static metamodel class for the specified entity.
	 *
	 * @param entityClass the entity for which to retrieve the metamodel class. Cannot be {@code null}.
	 *
	 * @return the static metamodel class for the specified entity.
	 */
	public static Class<?> getMetamodelClassFor(Class<?> entityClass, boolean prefix) {
		assertNotNull( entityClass, "Class parameter cannot be null" );
		String metaModelClassName = getMetaModelClassName( entityClass, prefix );
		try {
			URL outDirUrl = getOutBaseDir( entityClass ).toURI().toURL();
			URL[] urls = new URL[1];
			urls[0] = outDirUrl;
			URLClassLoader classLoader = new URLClassLoader( urls, TestUtil.class.getClassLoader() );
			return classLoader.loadClass( metaModelClassName );
		}
		catch (Exception e) {
			fail( metaModelClassName + " was not generated." );
		}
		// keep the compiler happy
		return null;
	}

	public static Class<?> getMetamodelClassFor(String className) {
		assertNotNull( "Class parameter cannot be null", className );
		String metaModelClassName = getMetaModelClassName( className );
		try {
			URL outDirUrl = getOutBaseDir( TestUtil.class ).toURI().toURL();
			URL[] urls = new URL[1];
			urls[0] = outDirUrl;
			URLClassLoader classLoader = new URLClassLoader( urls, TestUtil.class.getClassLoader() );
			return classLoader.loadClass( metaModelClassName );
		}
		catch (Exception e) {
			fail( metaModelClassName + " was not generated." );
		}
		// keep the compiler happy
		return null;
	}

	public static File getMetaModelSourceFileFor(Class<?> clazz, boolean prefix) {
		if ( clazz.isMemberClass() ) {
			return getMetaModelSourceFileFor( clazz.getEnclosingClass(), prefix );
		}
		String metaModelClassName = getMetaModelClassName(clazz, prefix);
		// generate the file name
		String fileName = metaModelClassName.replace( PACKAGE_SEPARATOR, PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		return new File( getOutBaseDir( clazz ), fileName );
	}

	public static File getMetaModelSourceFileFor(String className) {
		String metaModelClassName = getMetaModelClassName(className );
		// generate the file name
		String fileName = metaModelClassName.replace( PACKAGE_SEPARATOR, PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		return new File( getOutBaseDir( TestUtil.class ), fileName );
	}

	private static String getMetaModelClassName(Class<?> clazz, boolean prefix) {
		final String packageName = clazz.getPackageName();
		return prefix ? packageName + '.' + META_MODEL_CLASS_POSTFIX + clazz.getName().substring( packageName.length() + 1 )
				.replaceAll( "\\$", "\\$_" )
				: packageName + clazz.getName().substring( packageName.length() )
						.replaceAll( "\\$", "_\\$" ) + META_MODEL_CLASS_POSTFIX;
	}

	private static String getMetaModelClassName(String className) {
		final int index = className.lastIndexOf( '.' );
		final String packageName = className.substring( 0, index + 1 );
		return packageName + className.substring( packageName.length() ).replaceAll( "\\$", "_\\$" ) + META_MODEL_CLASS_POSTFIX;
	}

	public static String getMetaModelSourceAsString(Class<?> clazz) {
		return getMetaModelSourceAsString( clazz, false );
	}

	public static String getMetaModelSourceAsString(Class<?> clazz, boolean prefix) {
		return getSourceFileContent( getMetaModelSourceFileFor( clazz, prefix ) );
	}

	public static String getMetaModelSourceAsString(String className) {
		return getSourceFileContent( getMetaModelSourceFileFor( className ) );
	}

	private static String getSourceFileContent(File sourceFile) {
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader( new FileReader( sourceFile ) );
			try {
				String line;
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				while ( ( line = input.readLine() ) != null ) {
					contents.append( line );
					contents.append( System.lineSeparator() );
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}

		return contents.toString();
	}

	public static void dumpMetaModelSourceFor(Class<?> clazz) {
		log.info( "Dumping meta model source for " + clazz.getName() + ":" );
		log.info( getMetaModelSourceAsString( clazz ) );
	}

	public static Constructor<?>[] getConstructorsFromMetamodelFor(Class<?> entityClass) {
		Class<?> metaModelClass = getMetamodelClassFor( entityClass );
		return metaModelClass.getConstructors();
	}

	public static Field getFieldFromMetamodelFor(Class<?> entityClass, String fieldName) {
		return getFieldFromMetamodelFor(entityClass.getName(), fieldName);
	}

	public static Field getFieldFromMetamodelFor(String className, String fieldName) {
		Class<?> metaModelClass = getMetamodelClassFor( className );
		try {
			return metaModelClass.getDeclaredField( fieldName );
		}
		catch (NoSuchFieldException e) {
			return null;
		}
	}

	public static Method getMethodFromMetamodelFor(Class<?> entityClass, String methodName, Class<?>... params) {
		return getMethodFromMetamodelFor(entityClass.getName(), methodName, params);
	}

	public static Method getMethodFromMetamodelFor(String className, String methodName, Class<?>... params) {
		Class<?> metaModelClass = getMetamodelClassFor( className );
		try {
			return metaModelClass.getDeclaredMethod( methodName, params );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static String fcnToPath(String fcn) {
		return fcn.replace( PACKAGE_SEPARATOR, RESOURCE_SEPARATOR );
	}

	private static boolean hasNonDefaultConstructorInMetamodelFor(Class<?> clazz) {
		return Arrays.stream( getConstructorsFromMetamodelFor( clazz ) )
				.anyMatch( constructor -> constructor.getParameterCount() > 0 );
	}

	private static boolean hasFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		return getFieldFromMetamodelFor( clazz, fieldName ) != null;
	}

	private static boolean hasFieldInMetamodelFor(String className, String fieldName) {
		return getFieldFromMetamodelFor( className, fieldName ) != null;
	}

	private static boolean hasMethodInMetamodelFor(Class<?> clazz, String fieldName, Class<?>... params) {
		return getMethodFromMetamodelFor( clazz, fieldName, params ) != null;
	}

	private static boolean hasMethodInMetamodelFor(String className, String fieldName, Class<?>... params) {
		return getMethodFromMetamodelFor( className, fieldName, params ) != null;
	}

	private static String buildErrorString(String baseError, Class<?> clazz) {
		StringBuilder builder = new StringBuilder();
		builder.append( baseError );
		builder.append( ".\n\n" );
		builder.append( "Source code for " );
		builder.append( clazz.getName() );
		builder.append( "_.java:" );
		builder.append( "\n" );
		builder.append( getMetaModelSourceAsString( clazz ) );
		return builder.toString();
	}

	private static String buildErrorString(String baseError, String className) {
		StringBuilder builder = new StringBuilder();
		builder.append( baseError );
		builder.append( ".\n\n" );
		builder.append( "Source code for " );
		builder.append( className );
		builder.append( "_.java:" );
		builder.append( "\n" );
		builder.append( getMetaModelSourceAsString( className ) );
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

	private static void deleteFilesRecursive(File file) {
		if ( file.isDirectory() ) {
			for ( File c : file.listFiles() ) {
				deleteFilesRecursive( c );
			}
		}
		if ( !file.delete() ) {
			fail( "Unable to delete file: " + file );
		}
	}

	/**
	 * Returns the target directory of the build.
	 *
	 * @return the target directory of the build
	 */
	public static File getTargetDir(Class<?> currentTestClass) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		String currentTestClassName = currentTestClass.getName();
		int hopsToCompileDirectory = currentTestClassName.split( "\\." ).length;
		int hopsToTargetDirectory = hopsToCompileDirectory + 2;
		URL classURL = contextClassLoader.getResource( currentTestClassName.replace( '.', '/' ) + ".class" );
		// navigate back to '/target'
		File targetDir = new File( classURL.getFile() );
		// navigate back to '/target'
		for ( int i = 0; i < hopsToTargetDirectory; i++ ) {
			targetDir = targetDir.getParentFile();
		}
		return targetDir;
	}

	private static void assertCollectionAttributeTypeInMetaModelFor(
			Class<?> clazz,
			String fieldName,
			Class<?> collectionType,
			Class<?> expectedType,
			String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field, "Cannot find field '" + fieldName + "' in " + clazz.getName() );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type rawType = type.getRawType();

		assertEquals(
				collectionType,
				rawType,
				"Types do not match: " + buildErrorString( errorString, clazz )
		);

		Type genericType = type.getActualTypeArguments()[1];

		assertEquals(
				expectedType,
				genericType,
				"Types do not match: " + buildErrorString( errorString, clazz )
		);
	}

}
