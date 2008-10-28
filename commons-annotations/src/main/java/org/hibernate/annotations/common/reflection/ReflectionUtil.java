package org.hibernate.annotations.common.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;

/**
 * @author Paolo Perrotta
 */
public class ReflectionUtil {

    public static boolean isProperty(Method m, Type boundType, Filter filter) {
    	return ReflectionUtil.isPropertyType( boundType )
    			&& !m.isSynthetic()
    			&& !m.isBridge()
    			&& ( filter.returnStatic() || !Modifier.isStatic( m.getModifiers() ) )
    			&& m.getParameterTypes().length == 0
    			&& ( m.getName().startsWith( "get" ) || m.getName().startsWith( "is" ) );
    	// TODO should we use stronger checking on the naming of getters/setters, or just leave this to the validator?
    }

    public static boolean isProperty(Field f, Type boundType, Filter filter) {
    	return ( filter.returnStatic() || ! Modifier.isStatic( f.getModifiers() ) )
    			&& ( filter.returnTransient() || ! Modifier.isTransient( f.getModifiers() ) )
                && !f.isSynthetic()
                && ReflectionUtil.isPropertyType( boundType );
    }

    private static boolean isPropertyType(Type type) {
    //		return TypeUtils.isArray( type ) || TypeUtils.isCollection( type ) || ( TypeUtils.isBase( type ) && ! TypeUtils.isVoid( type ) );
    		return !TypeUtils.isVoid( type );
    	}
}
