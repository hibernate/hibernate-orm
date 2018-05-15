/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import java.util.Locale;

import org.hibernate.HibernateException;

/**
 * Indicates reference to a non-existing entity
 *
 * @author Steve Ebersole
 */
public class NotNavigableException extends HibernateException {
	private static final String MSG_TEMPLATE = "%s is not a navigable (managed-type or collection)";

	private final String name;

	public NotNavigableException(String name) {
		super( String.format( Locale.ROOT, MSG_TEMPLATE, name ) );
		this.name = name;
	}

	public NotNavigableException(String name, Throwable cause) {
		super( String.format( Locale.ROOT, MSG_TEMPLATE, name ), cause );
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
