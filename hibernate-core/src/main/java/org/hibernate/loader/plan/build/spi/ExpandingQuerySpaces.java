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
package org.hibernate.loader.plan.build.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.internal.spaces.CompositePropertyMapping;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.loader.plan.spi.QuerySpaces;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Models a collection of {@link QuerySpace} references and
 * exposes the ability to create an {@link ExpandingQuerySpace} for "returns" and fetches;
 * used when building a load plan.
 *
 * @author Steve Ebersole
 */
public interface ExpandingQuerySpaces extends QuerySpaces {

	/**
	 * Generate a unique ID to be used when creating an {@link ExpandingQuerySpace}.
	 * <p/>
	 * Using this method to generate a unique ID ensures that this object
	 * does not contain a {@link QuerySpace} with the returned unique ID.
	 *
	 * @return The unique ID.
	 */
	public String generateImplicitUid();

	/**
	 * Create an {@link ExpandingEntityQuerySpace} for an entity "return" with the
	 * specified unique ID.
	 *
	 * The unique ID should be generated using {@link #generateImplicitUid()},
	 *
	 * A unique suffix may be added to the unique ID for an existing {@link QuerySpace}.
	 * In this case, it is the caller's responsibility to ensure uniqueness.
	 *
	 * @param uid The unique ID for the root entity query space.
	 * @param entityPersister The entity persister.
	 * @return the {@link ExpandingEntityQuerySpace} with the specified unique ID.
	 * @throws IllegalStateException if there is already a query space with the
	 *         specified unique ID.
	 *
	 * @see org.hibernate.loader.plan.spi.EntityReturn
	 */
	public ExpandingEntityQuerySpace makeRootEntityQuerySpace(
			String uid,
			EntityPersister entityPersister);

	/**
	 * Create an {@link ExpandingEntityQuerySpace} for an entity (that is not a "return")
	 * with the specified unique ID.
	 *
	 * The unique ID should be generated using {@link #generateImplicitUid()},
	 *
	 * A unique suffix may be added to the unique ID for an existing {@link QuerySpace}.
	 * In this case, it is the caller's responsibility to ensure uniqueness.
	 *
	 * @param uid The unique ID for the entity query space.
	 * @param entityPersister The entity persister.
	 * @param canJoinsBeRequired <code>true</code> if joins added to the returned value
	 *                          can be required joins; <code>false</code>, otherwise.
	 *
	 * @return the {@link ExpandingEntityQuerySpace} with the specified unique ID.
	 * @throws IllegalStateException if there is already a query space with the
	 *         specified unique ID.
	 *
	 * @see Join
	 */
	public ExpandingEntityQuerySpace makeEntityQuerySpace(
			String uid,
			EntityPersister entityPersister,
			boolean canJoinsBeRequired);

	/**
	 * Create an {@link ExpandingCollectionQuerySpace} for a collection "return" with the
	 * specified unique ID.
	 *
	 * The unique ID should be generated using {@link #generateImplicitUid()},
	 *
	 * A unique suffix may be added to the unique ID for an existing {@link QuerySpace}.
	 * In this case, it is the caller's responsibility to ensure uniqueness.
	 *
	 * @param uid The unique ID for the root collection query space.
	 * @param collectionPersister The collection persister.
	 * @return the {@link ExpandingCollectionQuerySpace} with the specified unique ID.
	 * @throws IllegalStateException if there is already a query space with the
	 *         specified unique ID.
	 *
	 * @see org.hibernate.loader.plan.spi.CollectionReturn
	 */
	public ExpandingCollectionQuerySpace makeRootCollectionQuerySpace(
			String uid,
			CollectionPersister collectionPersister);

	/**
	 * Create an {@link ExpandingCollectionQuerySpace} for a collection (that is not a "return")
	 * with the specified unique ID.
	 *
	 * The unique ID should be generated using {@link #generateImplicitUid()},
	 *
	 * A unique suffix may be added to the unique ID for an existing {@link QuerySpace}.
	 * In this case, it is the caller's responsibility to ensure uniqueness.
	 *
	 * @param uid The unique ID for the collection query space.
	 * @param collectionPersister The collection persister.
	 * @param canJoinsBeRequired <code>true</code> if joins added to the returned value
	 *                          can be required joins; <code>false</code>, otherwise.
	 *
	 * @return the {@link ExpandingCollectionQuerySpace} with the specified unique ID.
	 * @throws IllegalStateException if there is already a query space with the
	 *         specified unique ID.
	 *
	 * @see Join
	 */
	public ExpandingCollectionQuerySpace makeCollectionQuerySpace(
			String uid,
			CollectionPersister collectionPersister,
			boolean canJoinsBeRequired);

	/**
	 * Create an {@link ExpandingCompositeQuerySpace} for a composite
	 * with the specified unique ID.
	 *
	 * The unique ID should be generated using {@link #generateImplicitUid()},
	 *
	 * A unique suffix may be added to the unique ID for an existing {@link QuerySpace}.
	 * In this case, it is the caller's responsibility to ensure uniqueness.
	 *
	 * @param uid The unique ID for the composite query space.
	 * @param compositePropertyMapping The composite property mapping.
	 * @param canJoinsBeRequired <code>true</code> if joins added to the returned value
	 *                          can be required joins; <code>false</code>, otherwise.
	 *
	 * @return the {@link ExpandingCompositeQuerySpace} with the specified unique ID.
	 * @throws IllegalStateException if there is already a query space with the
	 *         specified unique ID.
	 *
	 * @see Join
	 */
	public ExpandingCompositeQuerySpace makeCompositeQuerySpace(
			String uid,
			CompositePropertyMapping compositePropertyMapping,
			boolean canJoinsBeRequired);

	/**
	 * Gets the session factory.
	 *
	 * @return The session factory.
	 */
	public SessionFactoryImplementor getSessionFactory();
}
