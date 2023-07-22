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

	MethodKind getMethodKind();

	@Override
	default String resolveAttributeName() {
		return Introspector.decapitalize( resolveAttributeMethodNameStem() );
	}
}
