/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate;

import org.hibernate.internal.util.Loggable;

/**
 * Hibernate often deals with compound names/paths.  This interface
 * defines a standard way of interacting with them
 *
 * @author Steve Ebersole
 */
public interface DotIdentifierSequence extends Loggable {
	DotIdentifierSequence getParent();
	String getLocalName();
	String getFullPath();

	DotIdentifierSequence append(String subPathName);

	default boolean isRoot() {
		return getParent() == null;
	}

	@Override
	default String toLoggableFragment() {
		return getLocalName();
	}
}
