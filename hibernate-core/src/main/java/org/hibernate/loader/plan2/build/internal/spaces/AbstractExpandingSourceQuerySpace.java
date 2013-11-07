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
import org.hibernate.loader.plan2.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan2.build.spi.ExpandingSourceQuerySpace;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * @author Gail Badner
 */
public abstract class AbstractExpandingSourceQuerySpace extends AbstractQuerySpace implements ExpandingSourceQuerySpace {

	public AbstractExpandingSourceQuerySpace(
			String uid,
			Disposition disposition,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, disposition, querySpaces, canJoinsBeRequired, sessionFactory );
	}

	@Override
	public ExpandingCompositeQuerySpace addCompositeQuerySpace(
			CompositionDefinition compositionDefinition,
			String querySpaceUid,
			boolean shouldIncludeJoin) {
		final boolean required = canJoinsBeRequired() && !compositionDefinition.isNullable();

		final ExpandingCompositeQuerySpace rhs = getExpandingQuerySpaces().makeCompositeQuerySpace(
				querySpaceUid,
				new CompositePropertyMapping(
						compositionDefinition.getType(),
						getPropertyMapping(),
						compositionDefinition.getName()
				),
				required
		);

		if ( shouldIncludeJoin ) {
			final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
					this,
					compositionDefinition.getName(),
					rhs,
					required,
					compositionDefinition.getType()
			);
			internalGetJoins().add( join );
		}

		return rhs;
	}

	@Override
	public ExpandingEntityQuerySpace addEntityQuerySpace(
			AttributeDefinition attribute,
			EntityPersister persister,
			String querySpaceUid,
			boolean optional,
			boolean shouldIncludeJoin) {
		// TODO: Queryable.isMultiTable() may be more broad than it needs to be...
		final boolean isMultiTable = Queryable.class.cast( persister ).isMultiTable();
		final boolean required = canJoinsBeRequired() && !isMultiTable && !optional;

		final ExpandingEntityQuerySpace rhs = getExpandingQuerySpaces().makeEntityQuerySpace(
				querySpaceUid,
				persister,
				required
		);

		if ( shouldIncludeJoin ) {
			final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createEntityJoin(
					this,
					attribute.getName(),
					rhs,
					required,
					(EntityType) attribute.getType(),
					sessionFactory()
			);
			internalGetJoins().add( join );
		}

		return rhs;
	}

	@Override
	public ExpandingCollectionQuerySpace addCollectionQuerySpace(
			AttributeDefinition attributeDefinition,
			CollectionPersister collectionPersister,
			String querySpaceUid,
			boolean shouldIncludeJoin) {
		final boolean required = canJoinsBeRequired() && !attributeDefinition.isNullable();

		final ExpandingCollectionQuerySpace rhs = getExpandingQuerySpaces().makeCollectionQuerySpace(
				querySpaceUid,
				collectionPersister,
				required
		);

		if ( shouldIncludeJoin ) {
			final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCollectionJoin(
					this,
					attributeDefinition.getName(),
					rhs,
					required,
					(CollectionType) attributeDefinition.getType(),
					sessionFactory()
			);
			internalGetJoins().add( join );
		}

		return rhs;
	}

	@Override
	public ExpandingQuerySpaces getExpandingQuerySpaces() {
		return super.getExpandingQuerySpaces();
	}
}
