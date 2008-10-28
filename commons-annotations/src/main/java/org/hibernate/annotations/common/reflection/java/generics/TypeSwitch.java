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
package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * A visitor for the <code>java.lang.reflect.Type</code> hierarchy.
 *
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public class TypeSwitch<T> {

	public final T doSwitch(Type type) {
		if ( type instanceof Class ) {
			return caseClass( (Class) type );
		}
		if ( type instanceof GenericArrayType ) {
			return caseGenericArrayType( (GenericArrayType) type );
		}
		if ( type instanceof ParameterizedType ) {
			return caseParameterizedType( (ParameterizedType) type );
		}
		if ( type instanceof TypeVariable ) {
			return caseTypeVariable( (TypeVariable) type );
		}
		if ( type instanceof WildcardType ) {
			return caseWildcardType( (WildcardType) type );
		}
		return defaultCase( type );
	}

	public T caseWildcardType(WildcardType wildcardType) {
		return defaultCase( wildcardType );
	}

	public T caseTypeVariable(TypeVariable typeVariable) {
		return defaultCase( typeVariable );
	}

	public T caseClass(Class classType) {
		return defaultCase( classType );
	}

	public T caseGenericArrayType(GenericArrayType genericArrayType) {
		return defaultCase( genericArrayType );
	}

	public T caseParameterizedType(ParameterizedType parameterizedType) {
		return defaultCase( parameterizedType );
	}

	public T defaultCase(Type t) {
		return null;
	}
}
