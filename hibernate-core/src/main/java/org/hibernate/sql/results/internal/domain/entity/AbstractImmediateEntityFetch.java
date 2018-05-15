/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.Collections;
import java.util.List;

import org.hibernate.loader.spi.SingleEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.sql.results.spi.EntityFetch;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractImmediateEntityFetch implements EntityFetch {
	private final FetchParent fetchParent;
	private final EntityValuedNavigable fetchedNavigable;
	protected final SingleEntityLoader loader;

	protected AbstractImmediateEntityFetch(
			FetchParent fetchParent,
			EntityValuedNavigable fetchedNavigable,
			SingleEntityLoader loader) {
		this.fetchParent = fetchParent;
		this.fetchedNavigable = fetchedNavigable;
		this.loader = loader;
	}

	@Override
	public EntityValuedNavigable getEntityValuedNavigable() {
		return fetchedNavigable;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EntityValuedNavigable getFetchedNavigable() {
		return fetchedNavigable;
	}

	@Override
	public boolean isNullable() {
		return getFetchedNavigable().isNullable();
	}

	@Override
	public NavigableContainer getNavigableContainer() {
		return getFetchedNavigable().getContainer();
	}

	@Override
	public List<Fetch> getFetches() {
		return Collections.emptyList();
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getFetchedNavigable().getJavaTypeDescriptor();
	}
}
