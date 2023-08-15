/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance;

import org.hibernate.HibernateException;

import net.bytebuddy.description.type.TypeDescription;

/**
 * Indicates a condition where we know a class has been enhanced, but are
 * unable to determine the version of Hibernate used to perform the enhancement.
 *
 * @author Steve Ebersole
 */
public class UnknownVersionException extends HibernateException {
	private final String typeName;

	public UnknownVersionException(TypeDescription typeDescription) {
		super( "Could not determine Hibernate version used to enhance `" + typeDescription.getName() + "`" );

		this.typeName = typeDescription.getName();
	}

	public String getTypeName() {
		return typeName;
	}
}
