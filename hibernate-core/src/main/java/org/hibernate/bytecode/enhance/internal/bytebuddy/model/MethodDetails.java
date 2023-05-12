/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import java.beans.Introspector;

import org.hibernate.HibernateException;

/**
 * Models a "{@linkplain java.lang.reflect.Method method}" in a {@link ClassDetails}
 *
 * @author Steve Ebersole
 */
public interface MethodDetails extends MemberDetails {
	@Override
	default Kind getKind() {
		return Kind.METHOD;
	}

	enum MethodKind {
		GETTER,
		SETTER,
		OTHER
	}

	default MethodKind getMethodKind() {
		return null;
	}

	@Override
	default String resolveAttributeName() {
		final String methodName = getName();

		if ( methodName.startsWith( "is" ) ) {
			return Introspector.decapitalize( methodName.substring( 2 ) );
		}
		else if ( methodName.startsWith( "has" ) ) {
			return Introspector.decapitalize( methodName.substring( 3 ) );
		}
		else if ( methodName.startsWith( "get" ) ) {
			return Introspector.decapitalize( methodName.substring( 3 ) );
		}

		throw new HibernateException( "Could not determine attribute name from method - " + methodName );
	}
}
