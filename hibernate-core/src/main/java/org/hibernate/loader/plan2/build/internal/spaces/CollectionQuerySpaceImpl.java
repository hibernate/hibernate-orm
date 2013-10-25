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

import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan2.build.spi.AbstractQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class CollectionQuerySpaceImpl extends AbstractQuerySpace implements CollectionQuerySpace {
	private final CollectionPersister persister;
	private final CollectionPropertyMapping propertyMapping;

	public CollectionQuerySpaceImpl(
			CollectionPersister persister,
			String uid,
			QuerySpacesImpl querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.COLLECTION, querySpaces, canJoinsBeRequired, sessionFactory );
		this.persister = persister;
		this.propertyMapping = new CollectionPropertyMapping( (QueryableCollection) persister );
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return propertyMapping;
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
				CollectionPropertyNames.COLLECTION_INDICES,
				entityQuerySpace,
				Helper.INSTANCE.determineRhsColumnNames( (EntityType) persister.getIndexType(), sessionFactory() ),
				persister.getIndexType(),
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
				CollectionPropertyNames.COLLECTION_INDICES,
				compositeQuerySpace,
				( (QueryableCollection) persister ).getIndexColumnNames(),
				persister.getIndexType(),
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
				CollectionPropertyNames.COLLECTION_ELEMENTS,
				entityQuerySpace,
				Helper.INSTANCE.determineRhsColumnNames( (EntityType) persister.getElementType(), sessionFactory() ),
				persister.getElementType(),
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
				CollectionPropertyNames.COLLECTION_ELEMENTS,
				compositeQuerySpace,
				( (QueryableCollection) persister ).getElementColumnNames(),
				compositeType,
				canJoinsBeRequired()
		);
		internalGetJoins().add( join );

		return join;
	}
}
