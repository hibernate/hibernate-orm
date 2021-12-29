/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * Memento describing the parent of a fetch within a
 * named result-set mapping
 */
public interface FetchParentMemento {
	NavigablePath getNavigablePath();

	FetchableContainer getFetchableContainer();
}
