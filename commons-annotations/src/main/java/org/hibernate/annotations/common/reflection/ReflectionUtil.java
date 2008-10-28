/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
