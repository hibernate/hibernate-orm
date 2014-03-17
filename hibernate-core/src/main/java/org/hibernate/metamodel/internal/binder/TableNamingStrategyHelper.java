/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class TableNamingStrategyHelper implements ObjectNameNormalizer.LogicalNamingStrategyHelper {
	protected final EntityBinding entityBinding;
	protected final String entityName;
	protected String logicalName;

	TableNamingStrategyHelper(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
		this.entityName = getImplicitTableName();
	}

	@Override
	public String determineImplicitName(NamingStrategy strategy) {
		String name = getImplicitTableName();
		return strategy.classToTableName( name );
	}

	protected String getImplicitTableName() {
		return StringHelper.isNotEmpty( entityBinding.getJpaEntityName() )
				? entityBinding.getJpaEntityName()
				: entityBinding.getEntityName();
	}

	@Override
	public String handleExplicitName(NamingStrategy strategy, String tableName) {
		this.logicalName = tableName;
		return strategy.tableName( tableName );
	}

	@Override
	public String getLogicalName(NamingStrategy strategy) {
		return logicalName == null ? entityName : logicalName;
	}
}
