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

//$Id$
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;

/**
 * @author Emmanuel Bernard
 */
public abstract class JavaXMember extends JavaXAnnotatedElement implements XMember {
	private final Type type;
	private final TypeEnvironment env;
	private final JavaXType xType;

	protected static Type typeOf(Member member, TypeEnvironment env) {
		if ( member instanceof Field ) {
			return env.bind( ( (Field) member ).getGenericType() );
		}
		if ( member instanceof Method ) {
			return env.bind( ( (Method) member ).getGenericReturnType() );
		}
		throw new IllegalArgumentException( "Member " + member + " is neither a field nor a method" );
	}

	protected JavaXMember(Member member, Type type, TypeEnvironment env, JavaReflectionManager factory, JavaXType xType) {
		super( (AnnotatedElement) member, factory );
		this.type = type;
		this.env = env;
		this.xType = xType;
	}

	public XClass getType() {
		return xType.getType();
	}

	public abstract String getName();

	protected Type getJavaType() {
		return env.bind( type );
	}

	protected TypeEnvironment getTypeEnvironment() {
		return env;
	}

	protected Member getMember() {
		return (Member) toAnnotatedElement();
	}

	public Class<? extends Collection> getCollectionClass() {
		return xType.getCollectionClass();
	}

	public XClass getClassOrElementClass() {
		return xType.getClassOrElementClass();
	}

	public XClass getElementClass() {
		return xType.getElementClass();
	}

	public XClass getMapKey() {
		return xType.getMapKey();
	}

	public boolean isArray() {
		return xType.isArray();
	}

	public boolean isCollection() {
		return xType.isCollection();
	}

	public int getModifiers() {
		return getMember().getModifiers();
	}

	public final boolean isTypeResolved() {
		return xType.isResolved();
	}

	public void setAccessible(boolean accessible) {
		( (AccessibleObject) getMember() ).setAccessible( accessible );
	}
}
