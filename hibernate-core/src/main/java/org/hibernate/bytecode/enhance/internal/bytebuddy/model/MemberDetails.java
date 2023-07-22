/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import java.lang.reflect.Member;

/**
 * Details about a {@linkplain Member member} while processing annotations.
 *
 * @apiNote This can be a virtual member, meaning there is no physical
 * member in the declaring type (which itself might be virtual)
 *
 * @author Steve Ebersole
 */
public interface MemberDetails extends AnnotationTarget {

	/**
	 * The name of the member.  This would be the name of the method or field.
	 */
	String getName();

	/**
	 * The field type or method return type
	 */
	ClassDetails getType();

	/**
	 * For members representing attributes, determine the name stem for methods related
	 * to it.  This is the name of the attribute with its first letter capitalized.
	 * <p/>
	 * For an attribute named {@code text}, the name stem would be {@code Text} as a base for the
	 * {@code getText} and {@code setText} method names
	 */
	String resolveAttributeMethodNameStem();

	/**
	 * For members representing attributes, determine the attribute name
	 */
	String resolveAttributeName();
}
