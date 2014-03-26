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
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.relational.Table;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class CollectionTableNamingStrategyHelper extends TableNamingStrategyHelper {
	private final String propertyName;

	public CollectionTableNamingStrategyHelper(final AbstractPluralAttributeBinding pluralAttributeBinding) {
		super( pluralAttributeBinding.getContainer().seekEntityBinding() );
		this.propertyName = pluralAttributeBinding.getAttribute().getName();
	}

	@Override
	public String determineImplicitName(NamingStrategy strategy) {
		return strategy.collectionTableName(
				entityBinding.getEntityName(),
				strategy.classToTableName( entityBinding.getEntityName() ),
				null,
				null,
				propertyName

		);
	}

	@Override
	public String getLogicalName(NamingStrategy strategy) {
		return strategy.logicalCollectionTableName(
				logicalName,
				strategy.classToTableName( entityBinding.getEntityName() ),
				null,
				propertyName
		);
	}
}
