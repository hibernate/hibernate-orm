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
package org.hibernate.loader.plan2.build.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan2.build.spi.AbstractQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionQuerySpace;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
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
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.COLLECTION, querySpaces, sessionFactory );
		this.persister = persister;
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	public static interface CollectionIndexEntityJoin extends Join {
		public CollectionQuerySpaceImpl getCollectionQuerySpace();

		@Override
		CollectionQuerySpaceImpl getLeftHandSide();

		@Override
		EntityQuerySpaceImpl getRightHandSide();
	}

	public CollectionIndexEntityJoin addIndexEntityJoin(
			EntityPersister indexPersister,
			LoadPlanBuildingContext context) {
		final String entityQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final EntityQuerySpaceImpl entityQuerySpace = new EntityQuerySpaceImpl(
				indexPersister,
				entityQuerySpaceUid,
				getQuerySpaces(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( entityQuerySpace );

		final CollectionIndexEntityJoin join = new CollectionIndexEntityJoin() {
			@Override
			public CollectionQuerySpaceImpl getCollectionQuerySpace() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CollectionQuerySpaceImpl getLeftHandSide() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public EntityQuerySpaceImpl getRightHandSide() {
				return entityQuerySpace;
			}

			@Override
			public boolean isRightHandSideOptional() {
				return false;
			}
		};
		internalGetJoins().add( join );

		return join;
	}

	public static interface CollectionIndexCompositeJoin extends Join {
		public CollectionQuerySpaceImpl getCollectionQuerySpace();

		@Override
		CollectionQuerySpaceImpl getLeftHandSide();

		@Override
		CompositeQuerySpaceImpl getRightHandSide();
	}

	public CollectionIndexCompositeJoin addIndexCompositeJoin(
			CompositeType compositeType,
			LoadPlanBuildingContext context) {
		final String compositeQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final CompositeQuerySpaceImpl compositeQuerySpace = new CompositeQuerySpaceImpl(
				CollectionQuerySpaceImpl.this.getUid(),
				compositeType,
				compositeQuerySpaceUid,
				Disposition.COMPOSITE,
				getQuerySpaces(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( compositeQuerySpace );

		final CollectionIndexCompositeJoin join = new CollectionIndexCompositeJoin() {
			@Override
			public CollectionQuerySpaceImpl getCollectionQuerySpace() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CollectionQuerySpaceImpl getLeftHandSide() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CompositeQuerySpaceImpl getRightHandSide() {
				return compositeQuerySpace;
			}

			@Override
			public boolean isRightHandSideOptional() {
				return false;
			}
		};
		internalGetJoins().add( join );

		return join;
	}

	public static interface CollectionElementEntityJoin extends Join {
		public CollectionQuerySpaceImpl getCollectionQuerySpace();

		@Override
		CollectionQuerySpaceImpl getLeftHandSide();

		@Override
		EntityQuerySpaceImpl getRightHandSide();
	}

	public CollectionElementEntityJoin addElementEntityJoin(
			EntityPersister elementPersister,
			LoadPlanBuildingContext context) {
		final String entityQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final EntityQuerySpaceImpl entityQuerySpace = new EntityQuerySpaceImpl(
				elementPersister,
				entityQuerySpaceUid,
				getQuerySpaces(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( entityQuerySpace );

		final CollectionElementEntityJoin join = new CollectionElementEntityJoin() {
			@Override
			public CollectionQuerySpaceImpl getCollectionQuerySpace() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CollectionQuerySpaceImpl getLeftHandSide() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public EntityQuerySpaceImpl getRightHandSide() {
				return entityQuerySpace;
			}

			@Override
			public boolean isRightHandSideOptional() {
				return false;
			}
		};
		internalGetJoins().add( join );

		return join;
	}

	public static interface CollectionElementCompositeJoin extends Join {
		public CollectionQuerySpaceImpl getCollectionQuerySpace();

		@Override
		CollectionQuerySpaceImpl getLeftHandSide();

		@Override
		CompositeQuerySpaceImpl getRightHandSide();
	}

	public CollectionElementCompositeJoin addElementCompositeJoin(
			CompositeType compositeType,
			LoadPlanBuildingContext context) {
		final String compositeQuerySpaceUid = getQuerySpaces().generateImplicitUid();
		final CompositeQuerySpaceImpl compositeQuerySpace = new CompositeQuerySpaceImpl(
				CollectionQuerySpaceImpl.this.getUid(),
				compositeType,
				compositeQuerySpaceUid,
				Disposition.COMPOSITE,
				getQuerySpaces(),
				sessionFactory()
		);
		( (QuerySpacesImpl) context.getQuerySpaces() ).registerQuerySpace( compositeQuerySpace );

		final CollectionElementCompositeJoin join = new CollectionElementCompositeJoin() {
			@Override
			public CollectionQuerySpaceImpl getCollectionQuerySpace() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CollectionQuerySpaceImpl getLeftHandSide() {
				return CollectionQuerySpaceImpl.this;
			}

			@Override
			public CompositeQuerySpaceImpl getRightHandSide() {
				return compositeQuerySpace;
			}

			@Override
			public boolean isRightHandSideOptional() {
				return false;
			}
		};
		internalGetJoins().add( join );

		return join;
	}
}
