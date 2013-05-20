/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.internal;
import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.MappingException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinType;

/**
 * This class represents a joinable association.
 *
 * @author Gavin King
 * @author Gail Badner
 */
public abstract class AbstractJoinableAssociationImpl implements JoinableAssociation {
	private final PropertyPath propertyPath;
	private final Fetch currentFetch;
	private final EntityReference currentEntityReference;
	private final CollectionReference currentCollectionReference;
	private final JoinType joinType;
	private final String withClause;
	private final Map<String, Filter> enabledFilters;
	private final boolean hasRestriction;

	public AbstractJoinableAssociationImpl(
			Fetch currentFetch,
			EntityReference currentEntityReference,
			CollectionReference currentCollectionReference,
			String withClause,
			boolean hasRestriction,
			Map<String, Filter> enabledFilters) throws MappingException {
		this.propertyPath = currentFetch.getPropertyPath();
		if ( currentFetch.getFetchStrategy().getStyle() == FetchStyle.JOIN ) {
			joinType = currentFetch.isNullable() ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN;
		}
		else {
			joinType = JoinType.NONE;
		}
		this.currentFetch = currentFetch;
		this.currentEntityReference = currentEntityReference;
		this.currentCollectionReference = currentCollectionReference;
		this.withClause = withClause;
		this.hasRestriction = hasRestriction;
		this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	public Fetch getCurrentFetch() {
		return currentFetch;
	}

	@Override
	public EntityReference getCurrentEntityReference() {
		return currentEntityReference;
	}

	@Override
	public CollectionReference getCurrentCollectionReference() {
		return currentCollectionReference;
	}

	@Override
	public boolean hasRestriction() {
		return hasRestriction;
	}

	@Override
	public String getWithClause() {
		return withClause;
	}

	@Override
	public Map<String, Filter> getEnabledFilters() {
		return enabledFilters;
	}
}
