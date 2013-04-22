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
package org.hibernate.loader.spi;

import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;

/**
 * Represents a joinable (entity or collection) association.
 *
 * @author Gail Badner
 */
public interface JoinableAssociation {

	/**
	 * Returns the property path of the association.
	 *
 	 * @return the property path of the association.
	 */
	PropertyPath getPropertyPath();

	/**
	 * Returns the type of join used for the association.
	 *
	 * @return the type of join used for the association.
	 *
	 * @see JoinType
	 */
	JoinType getJoinType();

	/**
	 * Returns the current association fetch object.
	 *
	 * @return the current association fetch object.
	 *
	 * @see Fetch
	 */
	Fetch getCurrentFetch();

	/**
	 * Return the current {@link EntityReference}, or null if none.
     * <p/>
	 * If {@link #getCurrentFetch()} returns an
	 * {@link org.hibernate.loader.plan.spi.EntityFetch}, this method will
	 * return the same object as {@link #getCurrentFetch()}.
	 * <p/>
	 * If {@link #getCurrentFetch()} returns a
	 * {@link org.hibernate.loader.plan.spi.CollectionFetch} and
	 * the collection's owner is returned or fetched, this
	 * method will return the {@link EntityReference} that owns the
	 * {@link Fetch} returned by {@link #getCurrentFetch()};
	 * otherwise this method returns null.
	 *
	 * @return the current {@link EntityReference}, or null if none.
	 *
	 * @see #getCurrentFetch()
	 * @see Fetch
	 * @see org.hibernate.loader.plan.spi.CollectionFetch
	 * @see org.hibernate.loader.plan.spi.EntityFetch
	 * @see EntityReference
	 */
	EntityReference getCurrentEntityReference();

	/**
	 * Return the current {@link CollectionReference}, or null if none.
	 * <p/>
	 * If {@link #getCurrentFetch()} returns a
	 * {@link org.hibernate.loader.plan.spi.CollectionFetch}, this method
	 * will return the same object as {@link #getCurrentFetch()}.
	 * <p/>
	 * If {@link #getCurrentFetch()} returns an
	 * {@link org.hibernate.loader.plan.spi.EntityFetch} that is
	 * a collection element (or part of a composite collection element),
	 * and that collection is being returned or fetched, this
	 * method will return the {@link CollectionReference};
	 * otherwise this method returns null.
	 *
	 * @return the current {@link CollectionReference}, or null if none.
	 *
	 * @see #getCurrentFetch()
	 * @see Fetch
	 * @see org.hibernate.loader.plan.spi.EntityFetch
	 * @see org.hibernate.loader.plan.spi.CollectionFetch
	 * @see CollectionReference
	 */
	CollectionReference getCurrentCollectionReference();

	/**
	 * Returns the association type.
	 *
	 * @return the association type.
	 *
	 * @see AssociationType
	 */
	AssociationType getAssociationType();

	/**
	 * Return the persister for creating the join for the association.
	 *
	 * @return the persister for creating the join for the association.
	 */
	Joinable getJoinable();

	/**
	 * Is this a collection association?
	 *
	 * @return true, if this is a collection association; false otherwise.
	 */
	boolean isCollection();

	/**
	 * Does this association have a restriction?
	 *
	 * @return true if this association has a restriction; false, otherwise.
	 */
	boolean hasRestriction();

	/**
	 * Does this association have a many-to-many association
	 * with the specified association?
     *
	 * @param other - the other association.
	 * @return true, if this association has a many-to-many association
	 *         with the other association; false otherwise.
	 */
	boolean isManyToManyWith(JoinableAssociation other);

	/**
	 * Returns the with clause for this association.
	 *
	 * @return the with clause for this association.
	 */
	String getWithClause();

	/**
	 * Returns the filters that are enabled for this association.
	 *
	 * @return the filters that are enabled for this association.
	 *
	 * @see Filter
	 */
	Map<String,Filter> getEnabledFilters();
}
