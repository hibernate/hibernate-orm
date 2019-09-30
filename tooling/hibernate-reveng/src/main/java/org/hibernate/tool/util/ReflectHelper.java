package org.hibernate.tool.util;

public class ReflectHelper {

	public static Class<?> classForName(String name) throws ClassNotFoundException {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if ( classLoader != null ) {
				return classLoader.loadClass(name);
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name );
	}
	
}
