/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttribute {
	String getName();

	AccessType getAccessType();

	boolean isAccessTypeExplicit();

	MemberDetails getUnderlyingMember();

	FieldDetails getUnderlyingField();
	void setUnderlyingField(FieldDetails underlyingField);

	MethodDetails getUnderlyingGetter();

	MethodDetails getUnderlyingSetter();
}
