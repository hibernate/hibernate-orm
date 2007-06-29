package org.hibernate.bytecode.javassist;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.Serializable;

/**
 * @author Muga Nishizawa
 */
public class FastClass implements Serializable {

	private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

	private Class type;

	private FastClass() {
	}

	private FastClass(Class type) {
		this.type = type;
	}

	public Object invoke(
			String name,
	        Class[] parameterTypes,
	        Object obj,
	        Object[] args) throws InvocationTargetException {
		return this.invoke( this.getIndex( name, parameterTypes ), obj, args );
	}

	public Object invoke(
			int index,
	        Object obj,
	        Object[] args) throws InvocationTargetException {
		Method[] methods = this.type.getMethods();
		try {
			return methods[index].invoke( obj, args );
		}
		catch ( ArrayIndexOutOfBoundsException e ) {
			throw new IllegalArgumentException(
					"Cannot find matching method/constructor"
			);
		}
		catch ( IllegalAccessException e ) {
			throw new InvocationTargetException( e );
		}
	}

	public Object newInstance() throws InvocationTargetException {
		return this.newInstance( this.getIndex( EMPTY_CLASS_ARRAY ), null );
	}

	public Object newInstance(
			Class[] parameterTypes,
	        Object[] args) throws InvocationTargetException {
		return this.newInstance( this.getIndex( parameterTypes ), args );
	}

	public Object newInstance(
			int index,
	        Object[] args) throws InvocationTargetException {
		Constructor[] conss = this.type.getConstructors();
		try {
			return conss[index].newInstance( args );
		}
		catch ( ArrayIndexOutOfBoundsException e ) {
			throw new IllegalArgumentException( "Cannot find matching method/constructor" );
		}
		catch ( InstantiationException e ) {
			throw new InvocationTargetException( e );
		}
		catch ( IllegalAccessException e ) {
			throw new InvocationTargetException( e );
		}
	}

	public int getIndex(String name, Class[] parameterTypes) {
		Method[] methods = this.type.getMethods();
		boolean eq = true;
		for ( int i = 0; i < methods.length; ++i ) {
			if ( !Modifier.isPublic( methods[i].getModifiers() ) ) {
				continue;
			}
			if ( !methods[i].getName().equals( name ) ) {
				continue;
			}
			Class[] params = methods[i].getParameterTypes();
			if ( params.length != parameterTypes.length ) {
				continue;
			}
			eq = true;
			for ( int j = 0; j < params.length; ++j ) {
				if ( !params[j].equals( parameterTypes[j] ) ) {
					eq = false;
					break;
				}
			}
			if ( eq ) {
				return i;
			}
		}
		return -1;
	}

	public int getIndex(Class[] parameterTypes) {
		Constructor[] conss = this.type.getConstructors();
		boolean eq = true;
		for ( int i = 0; i < conss.length; ++i ) {
			if ( !Modifier.isPublic( conss[i].getModifiers() ) ) {
				continue;
			}
			Class[] params = conss[i].getParameterTypes();
			if ( params.length != parameterTypes.length ) {
				continue;
			}
			eq = true;
			for ( int j = 0; j < params.length; ++j ) {
				if ( !params[j].equals( parameterTypes[j] ) ) {
					eq = false;
					break;
				}
			}
			if ( eq ) {
				return i;
			}
		}
		return -1;
	}

	public int getMaxIndex() {
		Method[] methods = this.type.getMethods();
		int count = 0;
		for ( int i = 0; i < methods.length; ++i ) {
			if ( Modifier.isPublic( methods[i].getModifiers() ) ) {
				count++;
			}
		}
		return count;
	}

	public String getName() {
		return this.type.getName();
	}

	public Class getJavaClass() {
		return this.type;
	}

	public String toString() {
		return this.type.toString();
	}

	public int hashCode() {
		return this.type.hashCode();
	}

	public boolean equals(Object o) {
		if ( !( o instanceof FastClass ) ) {
			return false;
		}
		return this.type.equals( ( ( FastClass ) o ).type );
	}

	public static FastClass create(Class type) {
		FastClass fc = new FastClass( type );
		return fc;
	}
}
