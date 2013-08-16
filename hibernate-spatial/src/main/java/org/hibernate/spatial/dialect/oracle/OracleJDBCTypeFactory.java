/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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
 * <p/>
 * This factory creates the Oracle extension types using reflection in order to
 * avoid creating compile-time dependencies on the proprietary Oracle driver.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 3, 2010
 */
public class OracleJDBCTypeFactory implements SQLTypeFactory {

	private final Class<?> datumClass;
	private final Class<?> numberClass;
	private final Class<?> arrayClass;
	private final Class<?> structClass;
	private final Class<?> arrayDescriptorClass;
	private final Class<?> structDescriptorClass;
	private final Method structDescriptorCreator;
	private final Method arrayDescriptorCreator;
	private final Constructor<?> numberConstructor;
	private final Constructor<?> arrayConstructor;
	private final Constructor<?> structConstructor;
	private final ConnectionFinder connectionFinder;

	/**
	 * Constructs an instance.
	 *
	 * @param connectionFinder the {@code ConnectionFinder} the use for retrieving the {@code OracleConnection} instance.
	 */
	public OracleJDBCTypeFactory(ConnectionFinder connectionFinder) {
		if ( connectionFinder == null ) {
			throw new HibernateException( "ConnectionFinder cannot be null" );
		}
		this.connectionFinder = connectionFinder;
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

	private Constructor<?> findConstructor(Class clazz, Class<?>... arguments) {
		try {
			return clazz.getConstructor( arguments );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "Error finding constructor for oracle.sql type.", e );
		}
	}

	private Class<?> findClass(String name) {
		try {
			return ReflectHelper.classForName( name );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Class 'oracle.sql.Datum' not found on class path" );
		}
	}

	private Object[] findDescriptorCreator(String className) {
		try {
			final Class clazz = ReflectHelper.classForName( className );
			final Method m = clazz.getMethod( "createDescriptor", String.class, Connection.class );
			return new Object[] { clazz, m };
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Class 'StructDescriptor' not found on classpath" );
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( "Class 'StructDescriptor' has no method 'createDescriptor(String,Connection)'" );
		}
	}

	@Override
	public Struct createStruct(SDOGeometry geom, Connection conn) throws SQLException {
		Connection oracleConnection = null;
		try {
			oracleConnection = connectionFinder.find( conn );
		}
		catch ( FinderException e ) {
			throw new HibernateException( "Problem finding Oracle Connection", e );
		}

		final Object structDescriptor = createStructDescriptor( SDOGeometry.getTypeName(), oracleConnection );
		final Object[] attributes = createDatumArray( 5 );
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

	@Override
	public Array createElemInfoArray(ElemInfo elemInfo, Connection conn) {
		final Object arrayDescriptor = createArrayDescriptor( ElemInfo.TYPE_NAME, conn );
		return createArray( arrayDescriptor, conn, elemInfo.getElements() );
	}

	@Override
	public Array createOrdinatesArray(Ordinates ordinates, Connection conn) throws SQLException {
		final Object arrayDescriptor = createArrayDescriptor( Ordinates.TYPE_NAME, conn );
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
