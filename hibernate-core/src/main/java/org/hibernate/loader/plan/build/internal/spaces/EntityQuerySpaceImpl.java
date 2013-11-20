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
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EntityQuerySpaceImpl extends AbstractExpandingSourceQuerySpace implements ExpandingEntityQuerySpace {
	private final EntityPersister persister;

	public EntityQuerySpaceImpl(
			EntityPersister persister,
			String uid,
			ExpandingQuerySpaces querySpaces,
			boolean canJoinsBeRequired) {
		super( uid, Disposition.ENTITY, querySpaces, canJoinsBeRequired );
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
	public String[] toAliasedColumns(String alias, String propertyName) {
		return getPropertyMapping().toColumns( alias, propertyName );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public ExpandingCompositeQuerySpace makeCompositeIdentifierQuerySpace() {
		final String compositeQuerySpaceUid = getUid() + "-id";
		final ExpandingCompositeQuerySpace rhs = getExpandingQuerySpaces().makeCompositeQuerySpace(
				compositeQuerySpaceUid,
				new CompositePropertyMapping(
						(CompositeType) getEntityPersister().getIdentifierType(),
						(PropertyMapping) getEntityPersister(),
						getEntityPersister().getIdentifierPropertyName()
				),
				canJoinsBeRequired()
		);
		final JoinDefinedByMetadata join = JoinHelper.INSTANCE.createCompositeJoin(
				this,
				EntityPersister.ENTITY_ID,
				rhs,
				canJoinsBeRequired(),
				(CompositeType) persister.getIdentifierType()
		);
		internalGetJoins().add( join );

		return rhs;
	}
}
