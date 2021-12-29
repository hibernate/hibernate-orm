/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results;

import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.query.results.ResultSetMappingResolutionContext;

/**
 * Describes the parent ("owner") of a fetch defined in an {@code hbm.xml}
 * result-set mapping
 */
public interface HbmFetchParent {
	/**
	 * Resolve to a
	 */
	FetchParentMemento resolveParentMemento(ResultSetMappingResolutionContext resolutionContext);
}
