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
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.JoinDefinedByMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositeQuerySpaceImpl extends AbstractQuerySpace implements CompositeQuerySpace, ExpandingQuerySpace {
	private final String ownerUid;
	private final CompositeType compositeType;

	public CompositeQuerySpaceImpl(
			String ownerUid,
			CompositeType compositeType,
			String uid,
			Disposition disposition,
			QuerySpacesImpl querySpaces,
			SessionFactoryImplementor sessionFactory) {
		super( uid, disposition, querySpaces, sessionFactory );
		this.ownerUid = ownerUid;
		this.compositeType = compositeType;
	}

	@Override
	public JoinDefinedByMetadata addCompositeJoin(
			CompositionDefinition compositionDefinition, String querySpaceUid) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public JoinDefinedByMetadata addEntityJoin(
			AttributeDefinition attributeDefinition,
			EntityPersister persister,
			String querySpaceUid,
			boolean optional) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public JoinDefinedByMetadata addCollectionJoin(
			AttributeDefinition attributeDefinition, CollectionPersister collectionPersister, String querySpaceUid) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
