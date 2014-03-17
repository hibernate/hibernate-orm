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
import org.hibernate.cfg.ObjectNameNormalizer;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class ColumnNamingStrategyHelper implements ObjectNameNormalizer.NamingStrategyHelper {
	private final String defaultName;
	private final boolean isDefaultAttributeName;

	public ColumnNamingStrategyHelper(
			final String defaultName,
			final boolean isDefaultAttributeName) {
		this.defaultName = defaultName;
		this.isDefaultAttributeName = isDefaultAttributeName;
	}

	@Override
	public String determineImplicitName(NamingStrategy strategy) {
		if ( isDefaultAttributeName ) {
			return strategy.propertyToColumnName( defaultName );
		}
		else {
			return strategy.columnName( defaultName );
		}
	}

	@Override
	public String handleExplicitName(NamingStrategy strategy, String name) {
		return strategy.columnName( name );
	}
}
