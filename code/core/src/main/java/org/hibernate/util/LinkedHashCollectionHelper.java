//$Id: LinkedHashCollectionHelper.java 10100 2006-07-10 16:31:09Z steve.ebersole@jboss.com $
package org.hibernate.util;

import java.util.Map;
import java.util.Set;
import java.lang.reflect.Constructor;

import org.hibernate.AssertionFailure;

public final class LinkedHashCollectionHelper {

	private static final Class SET_CLASS;
	private static final Class MAP_CLASS;
	private static final Class[] CAPACITY_CTOR_SIG = new Class[] { int.class, float.class };
	private static final Constructor SET_CAPACITY_CTOR;
	private static final Constructor MAP_CAPACITY_CTOR;
	private static final float LOAD_FACTOR_V = .75f;
	private static final Float LOAD_FACTOR = new Float( LOAD_FACTOR_V );

	static {
		Class setClass;
		Class mapClass;
		Constructor setCtor;
		Constructor mapCtor;
		try {
			setClass = Class.forName( "java.util.LinkedHashSet" );
			mapClass = Class.forName( "java.util.LinkedHashMap" );
			setCtor = setClass.getConstructor( CAPACITY_CTOR_SIG );
			mapCtor = mapClass.getConstructor( CAPACITY_CTOR_SIG );
		}
		catch ( Throwable t ) {
			setClass = null;
			mapClass = null;
			setCtor = null;
			mapCtor = null;
		}
		SET_CLASS = setClass;
		MAP_CLASS = mapClass;
		SET_CAPACITY_CTOR = setCtor;
		MAP_CAPACITY_CTOR = mapCtor;
	}

	public static Set createLinkedHashSet() {
		try {
			return (Set) SET_CLASS.newInstance();
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashSet", e);
		}
	}

	public static Set createLinkedHashSet(int anticipatedSize) {
		if ( anticipatedSize <= 0 ) {
			return createLinkedHashSet();
		}
		int initialCapacity = anticipatedSize + (int)( anticipatedSize * LOAD_FACTOR_V );
		try {
			return ( Set ) SET_CAPACITY_CTOR.newInstance( new Object[] { new Integer( initialCapacity ), LOAD_FACTOR  } );
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashSet", e);
		}
	}

	public static Map createLinkedHashMap() {
		try {
			return (Map) MAP_CLASS.newInstance();
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashMap", e);
		}
	}

	public static Map createLinkedHashMap(int anticipatedSize) {
		if ( anticipatedSize <= 0 ) {
			return createLinkedHashMap();
		}
		int initialCapacity = anticipatedSize + (int)( anticipatedSize * LOAD_FACTOR_V );
		try {
			return ( Map ) MAP_CAPACITY_CTOR.newInstance( new Object[] { new Integer( initialCapacity ), LOAD_FACTOR } );
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashMap", e);
		}
	}

	private LinkedHashCollectionHelper() {}

}






