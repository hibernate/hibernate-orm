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
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EntityQuerySpaceImpl extends AbstractQuerySpace implements ExpandingQuerySpace, EntityQuerySpace {
	private final EntityPersister persister;

	public EntityQuerySpaceImpl(
			EntityPersister persister,
			String uid,
			QuerySpacesImpl querySpaces,
			SessionFactoryImplementor sessionFactory) {
		super( uid, Disposition.ENTITY, querySpaces, sessionFactory );
		this.persister = persister;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public JoinDefinedByMetadata addCompositeJoin(CompositionDefinition compositionDefinition, String querySpaceUid) {
		final CompositeQuerySpaceImpl rhs = new CompositeQuerySpaceImpl(
				getUid(),
				compositionDefinition.getType(),
				querySpaceUid,
				Disposition.COMPOSITE,
				getQuerySpaces(),
				sessionFactory()
		);
		final JoinDefinedByMetadata join = new JoinImpl( this, compositionDefinition, rhs, compositionDefinition.isNullable() );
		internalGetJoins().add( join );
		getQuerySpaces().registerQuerySpace( rhs );
		return join;
	}

	public JoinDefinedByMetadata addEntityJoin(
			AttributeDefinition attribute,
			EntityPersister persister,
			String querySpaceUid,
			boolean optional) {
		final EntityQuerySpaceImpl rhs = new EntityQuerySpaceImpl(
				persister,
				querySpaceUid,
				getQuerySpaces(),
				sessionFactory()
		);
		final JoinDefinedByMetadata join = new JoinImpl( this, attribute, rhs, optional );
		internalGetJoins().add( join );
		getQuerySpaces().registerQuerySpace( rhs );
		return join;
	}

	@Override
	public JoinDefinedByMetadata addCollectionJoin(
			AttributeDefinition attributeDefinition,
			CollectionPersister collectionPersister,
			String querySpaceUid) {
		final CollectionQuerySpaceImpl rhs = new CollectionQuerySpaceImpl(
				collectionPersister,
				querySpaceUid,
				getQuerySpaces(),
				sessionFactory()
		);
		final JoinDefinedByMetadata join = new JoinImpl( this, attributeDefinition, rhs, attributeDefinition.isNullable() );
		internalGetJoins().add( join );
		getQuerySpaces().registerQuerySpace( rhs );
		return join;
	}

}
