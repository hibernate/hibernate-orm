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
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.ReflectionUtil;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.generics.CompoundTypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
class JavaXClass extends JavaXAnnotatedElement implements XClass {

	private final TypeEnvironment context;
    private final Class clazz;

    public JavaXClass(Class clazz, TypeEnvironment env, JavaReflectionManager factory) {
		super( clazz, factory );
        this.clazz = clazz; //optimization
        this.context = env;
	}

	public String getName() {
		return toClass().getName();
	}

	public XClass getSuperclass() {
		return getFactory().toXClass( toClass().getSuperclass(),
				CompoundTypeEnvironment.create(
                        getTypeEnvironment(),
                        getFactory().getTypeEnvironment( toClass() )
				)
		);
	}

	public XClass[] getInterfaces() {
		Class[] classes = toClass().getInterfaces();
		int length = classes.length;
		XClass[] xClasses = new XClass[length];
        if (length != 0) {
            TypeEnvironment environment = CompoundTypeEnvironment.create(
                    getTypeEnvironment(),
                    getFactory().getTypeEnvironment( toClass() )
                    );
            for ( int index = 0; index < length ; index++ ) {
                xClasses[index] = getFactory().toXClass( classes[index], environment );
            }
        }
        return xClasses;
	}

	public boolean isInterface() {
		return toClass().isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract( toClass().getModifiers() );
	}

	public boolean isPrimitive() {
		return toClass().isPrimitive();
	}

	public boolean isEnum() {
		return toClass().isEnum();
	}

	private List<XProperty> getDeclaredFieldProperties(Filter filter) {
		List<XProperty> result = new LinkedList<XProperty>();
		for ( Field f : toClass().getDeclaredFields() ) {
			if ( ReflectionUtil.isProperty( f, getTypeEnvironment().bind( f.getGenericType() ), filter ) ) {
				result.add( getFactory().getXProperty( f, getTypeEnvironment() ) );
			}
		}
		return result;
	}

	private List<XProperty> getDeclaredMethodProperties(Filter filter) {
		List<XProperty> result = new LinkedList<XProperty>();
		for ( Method m : toClass().getDeclaredMethods() ) {
			if ( ReflectionUtil.isProperty( m, getTypeEnvironment().bind( m.getGenericReturnType() ), filter ) ) {
				result.add( getFactory().getXProperty( m, getTypeEnvironment() ) );
			}
		}
		return result;
	}

	public List<XProperty> getDeclaredProperties(String accessType) {
		return getDeclaredProperties( accessType, XClass.DEFAULT_FILTER );
	}

	public List<XProperty> getDeclaredProperties(String accessType, Filter filter) {
		if ( accessType.equals( ACCESS_FIELD ) ) {
			return getDeclaredFieldProperties( filter );
		}
		if ( accessType.equals( ACCESS_PROPERTY ) ) {
			return getDeclaredMethodProperties( filter );
		}
		throw new IllegalArgumentException( "Unknown access type " + accessType );
	}

	public List<XMethod> getDeclaredMethods() {
		List<XMethod> result = new LinkedList<XMethod>();
		for ( Method m : toClass().getDeclaredMethods() ) {
			result.add( getFactory().getXMethod( m, getTypeEnvironment() ) );
		}
		return result;
	}

	public Class<?> toClass() {
		return clazz;
	}

	public boolean isAssignableFrom(XClass c) {
		return toClass().isAssignableFrom( ( (JavaXClass) c ).toClass() );
	}

	boolean isArray() {
		return toClass().isArray();
	}

	TypeEnvironment getTypeEnvironment() {
		return context;
	}
    
    @Override
    public String toString() {
        return getName();
    }
}