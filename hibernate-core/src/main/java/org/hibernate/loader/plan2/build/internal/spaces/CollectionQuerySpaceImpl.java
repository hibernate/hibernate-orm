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
package org.hibernate.loader.plan2.build.internal.spaces;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan2.build.spi.AbstractQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CollectionQuerySpaceImpl extends AbstractQuerySpace implements CollectionQuerySpace {
	private final CollectionPersister persister;

	public CollectionQuerySpaceImpl(
			CollectionPersister persister,
			String uid,
			QuerySpacesImpl querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.COLLECTION, querySpaces, canJoinsBeRequired, sessionFactory );
		this.persister = persister;
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return (PropertyMapping) persister;
	}

	public JoinImpl addIndexEntityJoin(
			final EntityPersister indexPersister,
			LoadPlanBuildingContext context) {
		final boolean required = canJoinsBeRequired();
		final String entityQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final EntityQuerySpaceImpl entityQuerySpace = new EntityQuerySpaceImpl(
				indexPersister,
				entityQuerySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( entityQuerySpace );

		final JoinImpl join = new JoinImpl(
				this,
				"index",
				entityQuerySpace,
				// not sure this 'rhsColumnNames' bit is correct...
				( (Queryable) indexPersister ).getKeyColumnNames(),
				required
		);
		internalGetJoins().add( join );

		return join;
	}

	public JoinImpl addIndexCompositeJoin(
			CompositeType compositeType,
			LoadPlanBuildingContext context) {
		final String compositeQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final CompositeQuerySpaceImpl compositeQuerySpace = new CompositeQuerySpaceImpl(
				new CompositePropertyMapping(
						compositeType,
						(PropertyMapping) getCollectionPersister(),
						"index"
				),
				compositeQuerySpaceUid,
				getQuerySpaces(),
				canJoinsBeRequired(),
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( compositeQuerySpace );

		final JoinImpl join = new JoinImpl(
				this,
				"index",
				compositeQuerySpace,
				null,
				canJoinsBeRequired()
		);
		internalGetJoins().add( join );

		return join;
	}

	public JoinImpl addElementEntityJoin(
			final EntityPersister elementPersister,
			LoadPlanBuildingContext context) {
		final String entityQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final EntityQuerySpaceImpl entityQuerySpace = new EntityQuerySpaceImpl(
				elementPersister,
				entityQuerySpaceUid,
				getQuerySpaces(),
				canJoinsBeRequired(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( entityQuerySpace );

		final JoinImpl join = new JoinImpl(
				this,
				// collection persister maps its elements (through its PropertyMapping contract) as non-prefixed
				"id",
				entityQuerySpace,
				( (Queryable) elementPersister ).getKeyColumnNames(),
				canJoinsBeRequired()
		);
		internalGetJoins().add( join );

		return join;
	}

	public Join addElementCompositeJoin(
			CompositeType compositeType,
			LoadPlanBuildingContext context) {
		final String compositeQuerySpaceUid = getQuerySpaces().generateImplicitUid();

		final CompositeQuerySpaceImpl compositeQuerySpace = new CompositeQuerySpaceImpl(
				new CompositePropertyMapping(
						compositeType,
						(PropertyMapping) getCollectionPersister(),
						""
				),
				compositeQuerySpaceUid,
				getQuerySpaces(),
				canJoinsBeRequired(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( compositeQuerySpace );

		final JoinImpl join = new JoinImpl(
				this,
				// collection persister maps its elements (through its PropertyMapping contract) as non-prefixed
				"elements",
				compositeQuerySpace,
				null,
				canJoinsBeRequired()
		);
		internalGetJoins().add( join );

		return join;
	}
}
