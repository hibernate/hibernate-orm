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
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityQuerySpaceImpl extends AbstractQuerySpace implements ExpandingEntityQuerySpace, EntityQuerySpace {
	private final EntityPersister persister;

	public EntityQuerySpaceImpl(
			EntityPersister persister,
			String uid,
			QuerySpacesImpl querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.ENTITY, querySpaces, canJoinsBeRequired, sessionFactory );
		this.persister = persister;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return super.sessionFactory();
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		// entity persisters are typically PropertyMapping implementors, but this is part of the funky
		// "optional interface hierarchy" for entity persisters.  The internal ones all implement
		// PropertyMapping...
		return (PropertyMapping) persister;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public JoinImpl addCompositeJoin(CompositionDefinition compositionDefinition, String querySpaceUid) {
		final boolean required = canJoinsBeRequired() && !compositionDefinition.isNullable();

		final CompositeQuerySpaceImpl rhs = new CompositeQuerySpaceImpl(
				new CompositePropertyMapping(
						compositionDefinition.getType(),
						(PropertyMapping) this.getEntityPersister(),
						compositionDefinition.getName()
				),
				querySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinImpl join = new JoinImpl(
				this,
				compositionDefinition.getName(),
				rhs,
				null,
				null,
				required
		);
		internalGetJoins().add( join );

		return join;
	}

	public JoinImpl addEntityJoin(
			AttributeDefinition attribute,
			EntityPersister persister,
			String querySpaceUid,
			boolean optional) {
		final boolean required = canJoinsBeRequired() && !optional;

		final EntityQuerySpaceImpl rhs = new EntityQuerySpaceImpl(
				persister,
				querySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinImpl join = new JoinImpl(
				this,
				attribute.getName(),
				rhs,
				Helper.INSTANCE.determineRhsColumnNames(
						(EntityType) attribute.getType(),
						sessionFactory()
				),
				(AssociationType) attribute.getType(),
				required
		);
		internalGetJoins().add( join );

		return join;
	}

	@Override
	public JoinImpl addCollectionJoin(
			AttributeDefinition attributeDefinition,
			CollectionPersister collectionPersister,
			String querySpaceUid) {
		final boolean required = canJoinsBeRequired() && !attributeDefinition.isNullable();

		final CollectionQuerySpaceImpl rhs = new CollectionQuerySpaceImpl(
				collectionPersister,
				querySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinImpl join = new JoinImpl(
				this,
				attributeDefinition.getName(),
				rhs,
				( (CollectionType) attributeDefinition.getType() ).getAssociatedJoinable( sessionFactory() ).getKeyColumnNames(),
				(AssociationType) attributeDefinition.getType(),
				required
		);
		internalGetJoins().add( join );

		return join;
	}

	@Override
	public Join makeCompositeIdentifierJoin() {
		final String compositeQuerySpaceUid = getUid() + "-id";
		final CompositeQuerySpaceImpl rhs = new CompositeQuerySpaceImpl(
				this,
				new CompositePropertyMapping(
						(CompositeType) getEntityPersister().getIdentifierType(),
						(PropertyMapping) getEntityPersister(),
						getEntityPersister().getIdentifierPropertyName()
				),
				compositeQuerySpaceUid,
				canJoinsBeRequired()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinImpl join = new JoinImpl(
				this,
				"id",
				rhs,
				null,
				null,
				canJoinsBeRequired()
		);
		internalGetJoins().add( join );

		return join;
	}
}
