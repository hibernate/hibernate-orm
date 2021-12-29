/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * FetchParentMemento implementation for {@code hbm.xml} defined
 * result-set mappings
 *
 * @author Steve Ebersole
 */
public class HbmFetchParentMemento implements FetchParentMemento {
	private final NavigablePath navigablePath;
	private final FetchableContainer fetchableContainer;

	public HbmFetchParentMemento(
			NavigablePath navigablePath,
			FetchableContainer fetchableContainer) {
		this.navigablePath = navigablePath;
		this.fetchableContainer = fetchableContainer;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public FetchableContainer getFetchableContainer() {
		return fetchableContainer;
	}

	@Override
	public String toString() {
		return "HbmFetchParentMemento(" + navigablePath.getFullPath() + ")";
	}
}
