/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.cfg.naming;

import java.io.Serializable;

import org.hibernate.internal.util.StringHelper;

/**
 * An "adapter" for {@link NamingStrategyDelegate} implementations to extend.
 *
 * @author Gail Badner
 */
public abstract class NamingStrategyDelegateAdapter implements NamingStrategyDelegate, Serializable {

	@Override
	public String determineImplicitPropertyColumnName(String propertyPath) {
		return StringHelper.unqualify( propertyPath );
	}

	@Override
	public String toPhysicalTableName(String tableName) {
		return tableName;
	}

	@Override
	public String toPhysicalColumnName(String columnName) {
		return columnName;
	}

	@Override
	public String toPhysicalJoinKeyColumnName(String joinedColumn, String joinedTable) {
		return toPhysicalColumnName( joinedColumn );
	}

	@Override
	public String determineLogicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : StringHelper.unqualify( propertyName );
	}

	@Override
	public String determineLogicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
		return StringHelper.isNotEmpty( columnName ) ?
				columnName :
				StringHelper.unqualify( propertyName ) + "_" + referencedColumn;
	}
}
