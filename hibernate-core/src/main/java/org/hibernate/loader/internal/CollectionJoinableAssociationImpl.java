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
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.AssociationType;

/**
 * This class represents a joinable collection association.
 *
 * @author Gail Badner
 */

public class CollectionJoinableAssociationImpl extends AbstractJoinableAssociationImpl {

	private final AssociationType joinableType;
	private final Joinable joinable;

	public CollectionJoinableAssociationImpl(
			CollectionFetch collectionFetch,
			EntityReference currentEntityReference,
			String withClause,
			boolean hasRestriction,
			Map<String, Filter> enabledFilters) throws MappingException {
		super(
				collectionFetch,
				currentEntityReference,
				collectionFetch,
				withClause,
				hasRestriction,
				enabledFilters
		);
		this.joinableType = collectionFetch.getCollectionPersister().getCollectionType();
		this.joinable = (Joinable) collectionFetch.getCollectionPersister();
	}

	@Override
	public AssociationType getAssociationType() {
		return joinableType;
	}

	@Override
	public Joinable getJoinable() {
		return joinable;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public boolean isManyToManyWith(JoinableAssociation other) {
		QueryableCollection persister = ( QueryableCollection ) joinable;
		if ( persister.isManyToMany() ) {
			return persister.getElementType() == other.getAssociationType();
		}
		return false;
	}

	protected boolean isOneToOne() {
		return false;
	}
}
