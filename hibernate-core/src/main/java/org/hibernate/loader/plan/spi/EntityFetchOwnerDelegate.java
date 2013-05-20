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
package org.hibernate.loader.plan.spi;

import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * @author Gail Badner
 */
public class EntityFetchOwnerDelegate implements FetchOwnerDelegate {
	private final EntityPersister entityPersister;

	public EntityFetchOwnerDelegate(EntityPersister entityPersister) {
		this.entityPersister = entityPersister;
	}

	@Override
	public boolean isNullable(Fetch fetch) {
		return entityPersister.getPropertyNullability()[ determinePropertyIndex( fetch ) ];
	}

	@Override
	public Type getType(Fetch fetch) {
		return entityPersister.getPropertyTypes()[ determinePropertyIndex( fetch ) ];
	}

	@Override
	public String[] getColumnNames(Fetch fetch) {
		final OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) entityPersister;
		Type fetchType = getType( fetch );
		if ( fetchType.isAssociationType() ) {
			return JoinHelper.getLHSColumnNames(
					(AssociationType) fetchType,
					determinePropertyIndex( fetch ),
					outerJoinLoadable,
					outerJoinLoadable.getFactory()
			);
		}
		else {
			return outerJoinLoadable.getPropertyColumnNames( determinePropertyIndex( fetch ) );
		}
	}

	private int determinePropertyIndex(Fetch fetch) {
		return entityPersister.getEntityMetamodel().getPropertyIndex( fetch.getOwnerPropertyName() );
	}
}
