package org.hibernate.spatial.dialect.oracle;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.spatial.helper.FinderException;

/**
 * Factory for Oracle JDBC extension types (ARRAY, STRUCT, ...).
 *
 * This factory creates the Oracle extension types using reflection in order to
 * avoid creating compile-time dependencies on the proprietary Oracle driver.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 3, 2010
 */
public class OracleJDBCTypeFactory extends SQLTypeFactory {

	private static Class<?> datumClass;
	private static Class<?> numberClass;
	private static Class<?> arrayClass;
	private static Class<?> structClass;
	private static Class<?> arrayDescriptorClass;
	private static Class<?> structDescriptorClass;
	private static Method structDescriptorCreator;
	private static Method arrayDescriptorCreator;
	private static Constructor<?> numberConstructor;
	private static Constructor<?> arrayConstructor;
	private static Constructor<?> structConstructor;


	static {
		Object[] obj = findDescriptorCreator( "oracle.sql.StructDescriptor" );
		structDescriptorClass = (Class<?>) obj[0];
		structDescriptorCreator = (Method) obj[1];
		obj = findDescriptorCreator( "oracle.sql.ArrayDescriptor" );
		arrayDescriptorClass = (Class<?>) obj[0];
		arrayDescriptorCreator = (Method) obj[1];
		datumClass = findClass( "oracle.sql.Datum" );
		numberClass = findClass( "oracle.sql.NUMBER" );
		arrayClass = findClass( "oracle.sql.ARRAY" );
		structClass = findClass( "oracle.sql.STRUCT" );

		numberConstructor = findConstructor( numberClass, java.lang.Integer.TYPE );
		arrayConstructor = findConstructor( arrayClass, arrayDescriptorClass, Connection.class, Object.class );
		structConstructor = findConstructor( structClass, structDescriptorClass, Connection.class, Object[].class );
	}

	private static ConnectionFinder connectionFinder = new DefaultConnectionFinder();

	private static Constructor<?> findConstructor(Class clazz, Class<?>... arguments) {
		try {
			return clazz.getConstructor( arguments );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "Error finding constructor for oracle.sql type.", e );
		}
	}

	private static Class<?> findClass(String name) {
		try {
			return ReflectHelper.classForName( name );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Class 'oracle.sql.Datum' not found on class path" );
		}
	}

	private static Object[] findDescriptorCreator(String className) {
		try {
			Class clazz = ReflectHelper.classForName( className );
			Method m = clazz.getMethod(
					"createDescriptor",
					String.class,
					Connection.class
			);
			return new Object[] { clazz, m };
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Class 'StructDescriptor' not found on classpath" );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "Class 'StructDescriptor' has no method 'createDescriptor(String,Connection)'" );
		}
	}

	static ConnectionFinder getConnectionFinder() {
		return connectionFinder;
	}

	static void setConnectionFinder(ConnectionFinder finder) {
		connectionFinder = finder;
	}


	public Struct createStruct(SDOGeometry geom, Connection conn) throws SQLException {
		Connection oracleConnection = null;
		try {
			oracleConnection = connectionFinder.find( conn );
		}
		catch ( FinderException e ) {
			throw new HibernateException( "Problem finding Oracle Connection", e );
		}

		Object structDescriptor = createStructDescriptor( SDOGeometry.getTypeName(), oracleConnection );
		Object[] attributes = createDatumArray( 5 );
		attributes[0] = createNumber( geom.getGType().intValue() );
		if ( geom.getSRID() > 0 ) {
			attributes[1] = createNumber( geom.getSRID() );
		}
		else {
			attributes[1] = null;
		}
		attributes[3] = createElemInfoArray( geom.getInfo(), oracleConnection );
		attributes[4] = createOrdinatesArray( geom.getOrdinates(), oracleConnection );
		return createStruct( structDescriptor, oracleConnection, attributes );
	}

	public Array createElemInfoArray(ElemInfo elemInfo, Connection conn) {
		Object arrayDescriptor = createArrayDescriptor( ElemInfo.TYPE_NAME, conn );
		return createArray( arrayDescriptor, conn, elemInfo.getElements() );
	}


	public Array createOrdinatesArray(Ordinates ordinates, Connection conn) throws SQLException {
		Object arrayDescriptor = createArrayDescriptor( Ordinates.TYPE_NAME, conn );
		return createArray( arrayDescriptor, conn, ordinates.getOrdinateArray() );

	}


	private Array createArray(Object descriptor, Connection conn, Object[] data) {
		try {
			return (Array) arrayConstructor.newInstance( descriptor, conn, data );
		}
		catch ( InstantiationException e ) {
			throw new HibernateException( "Problem creating ARRAY.", e );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Problem creating ARRAY.", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Problem creating ARRAY.", e );
		}
	}

	private Struct createStruct(Object descriptor, Connection conn, Object[] attributes) {
		try {
			return (Struct) structConstructor.newInstance( descriptor, conn, attributes );
		}
		catch ( InstantiationException e ) {
			throw new HibernateException( "Problem creating STRUCT.", e );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Problem creating STRUCT.", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Problem creating STRUCT.", e );
		}
	}

	private Object createStructDescriptor(String sqlType, Connection conn) {
		try {
			return structDescriptorCreator.invoke( null, sqlType, conn );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Error creating oracle STRUCT", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Error creating oracle STRUCT", e );
		}
	}

	private Object createArrayDescriptor(String name, Connection conn) {
		try {
			return arrayDescriptorCreator.invoke( null, name, conn );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Error creating oracle ARRAY", e );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Error creating oracle ARRAY", e );
		}
	}

	private Object[] createDatumArray(int size) {
		return (Object[]) java.lang.reflect.Array.newInstance( datumClass, size );

	}

	private Object createNumber(int obj) {
		try {
			return numberConstructor.newInstance( obj );
		}
		catch ( InvocationTargetException e ) {
			throw new HibernateException( "Error creating oracle NUMBER", e );
		}
		catch ( InstantiationException e ) {
			throw new HibernateException( "Error creating oracle NUMBER", e );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Error creating oracle NUMBER", e );
		}
	}

}
