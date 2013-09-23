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
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class CompositeQuerySpaceImpl extends AbstractQuerySpace implements ExpandingCompositeQuerySpace {
	private final CompositePropertyMapping compositeSubPropertyMapping;

	public CompositeQuerySpaceImpl(
			CompositePropertyMapping compositeSubPropertyMapping,
			String uid,
			QuerySpacesImpl querySpaces,
			boolean canJoinsBeRequired,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.COMPOSITE, querySpaces, canJoinsBeRequired, sessionFactory );
		this.compositeSubPropertyMapping = compositeSubPropertyMapping;
	}

	public CompositeQuerySpaceImpl(
			EntityQuerySpaceImpl entityQuerySpace,
			CompositePropertyMapping compositePropertyMapping,
			String uid,
			boolean canJoinsBeRequired) {
		// todo : we may need to keep around the owning entity query space to be able to properly handle circularity...
		this(
				compositePropertyMapping,
				uid,
				entityQuerySpace.getQuerySpaces(),
				canJoinsBeRequired,
				entityQuerySpace.sessionFactory()
		);
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return compositeSubPropertyMapping;
	}

	@Override
	public JoinImpl addCompositeJoin(CompositionDefinition compositionDefinition, String querySpaceUid) {
		final String propertyPath = compositionDefinition.getName();
		final boolean required = canJoinsBeRequired() && !compositionDefinition.isNullable();

		final CompositeQuerySpaceImpl rhs = new CompositeQuerySpaceImpl(
				compositeSubPropertyMapping,
				querySpaceUid,
				getQuerySpaces(),
				required,
				sessionFactory()
		);
		getQuerySpaces().registerQuerySpace( rhs );

		final JoinImpl join = new JoinImpl(
				this,
				propertyPath,
				rhs,
				null,
				null,
				required
		);
		internalGetJoins().add( join );

		return join;
	}

	@Override
	public JoinImpl addEntityJoin(
			AttributeDefinition attributeDefinition,
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

		final String propertyPath = attributeDefinition.getName();
		final JoinImpl join = new JoinImpl(
				this,
				propertyPath,
				rhs,
				Helper.INSTANCE.determineRhsColumnNames(
						(EntityType) attributeDefinition.getType(),
						sessionFactory()
				),
				(EntityType) attributeDefinition.getType(),
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
		final boolean required = canJoinsBeRequired() && ! attributeDefinition.isNullable();

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
}
