/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Table;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class ManyToManyCollectionTableNamingStrategyHelper extends TableNamingStrategyHelper {
	private final EntityBinding ownerEntityBinding;
	private final EntityBinding inverseEntityBinding;
	private final String ownerTableLogicalName;
	private final String inverseTableLogicalName;
	private final String propertyName;

	public ManyToManyCollectionTableNamingStrategyHelper(
			final String attributePath,
			final boolean isInverse,
			final EntityBinding entityBinding,
			final EntityBinding associatedEntityBinding) {
		super( entityBinding );
		if ( isInverse ) {
			ownerEntityBinding = associatedEntityBinding;
			inverseEntityBinding = entityBinding;
		}
		else {
			ownerEntityBinding = entityBinding;
			inverseEntityBinding = associatedEntityBinding;
		}
		ownerTableLogicalName =
				Table.class.isInstance( ownerEntityBinding.getPrimaryTable() )
						? ( (Table) ownerEntityBinding.getPrimaryTable() ).getPhysicalName().getText()
						: null;
		inverseTableLogicalName =
				Table.class.isInstance( inverseEntityBinding.getPrimaryTable() )
						? ( (Table) inverseEntityBinding.getPrimaryTable() ).getPhysicalName().getText()
						: null;
		propertyName = attributePath;
	}

	@Override
	public String determineImplicitName(NamingStrategy strategy) {
		return strategy.collectionTableName(
				ownerEntityBinding.getEntityName(),
				ownerTableLogicalName,
				inverseEntityBinding.getEntityName(),
				inverseTableLogicalName,
				propertyName
		);
	}

	@Override
	public String getLogicalName(NamingStrategy strategy) {
		return strategy.logicalCollectionTableName(
				logicalName,
				ownerEntityBinding.getPrimaryTable().getLogicalName().getText(),
				inverseEntityBinding.getPrimaryTable().getLogicalName().getText(),
				propertyName
		);
	}
}
