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
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityQuerySpaceImpl extends AbstractQuerySpace implements ExpandingEntityQuerySpace {
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
	public JoinDefinedByMetadata addCompositeJoin(CompositionDefinition compositionDefinition, String querySpaceUid) {
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

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				compositionDefinition.getName(),
				rhs,
				required,
				compositionDefinition.getType()
		);
		internalGetJoins().add( join );

		return join;
	}

	public JoinDefinedByMetadata addEntityJoin(
			AttributeDefinition attribute,
			EntityPersister persister,
			String querySpaceUid,
			boolean optional) {
		// TODO: Queryable.isMultiTable() may be more broad than it needs to be...
		final boolean isMultiTable = Queryable.class.cast( persister ).isMultiTable();
		final boolean required = canJoinsBeRequired() && !isMultiTable && !optional;

		final EntityQuerySpaceImpl rhs = new EntityQuerySpaceImpl(
				persister,
				querySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createEntityJoin(
				this,
				attribute.getName(),
				rhs,
				required,
				(EntityType) attribute.getType(),
				sessionFactory()
		);

		internalGetJoins().add( join );

		return join;
	}

	@Override
	public JoinDefinedByMetadata addCollectionJoin(
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
		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCollectionJoin(
				this,
				attributeDefinition.getName(),
				rhs,
				required,
				(CollectionType) attributeDefinition.getType(),
				sessionFactory()
		);
		internalGetJoins().add( join );

		return join;
	}

	@Override
	public JoinDefinedByMetadata makeCompositeIdentifierJoin() {
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

		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				EntityPersister.ENTITY_ID,
				rhs,
				canJoinsBeRequired(),
				(CompositeType) persister.getIdentifierType()
		);
		internalGetJoins().add( join );

		return join;
	}
}
