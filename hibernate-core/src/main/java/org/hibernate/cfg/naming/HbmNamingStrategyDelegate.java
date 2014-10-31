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

import org.hibernate.internal.util.StringHelper;

/**
 * @author Gail Badner
 */
public class HbmNamingStrategyDelegate extends NamingStrategyDelegateAdapter {

	@Override
	public String determineImplicitPrimaryTableName(String entityName, String jpaEntityName) {
		return StringHelper.unqualify( entityName );
	}

	@Override
	public String determineImplicitElementCollectionTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyPath) {
		return ownerEntityTable
				+ '_'
				+ StringHelper.unqualify( propertyPath );
	}

	@Override
	public String determineImplicitElementCollectionJoinColumnName(String ownerEntityName, String ownerJpaEntityName, String ownerEntityTable, String referencedColumnName, String propertyPath) {
		throw new UnsupportedOperationException( "Method not supported for Hibernate-specific mappings" );
	}

	@Override
	public String determineImplicitEntityAssociationJoinTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyPath) {
		return ownerEntityTable
				+ '_'
				+ StringHelper.unqualify( propertyPath );
	}

	@Override
	public String determineImplicitEntityAssociationJoinColumnName(
			String propertyEntityName, String propertyJpaEntityName, String propertyTableName, String referencedColumnName, String propertyPath) {
		throw new UnsupportedOperationException( "Method not supported for Hibernate-specific mappings" );
	}

	@Override
	public String determineLogicalElementCollectionTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyName) {
		if ( tableName != null ) {
			return tableName;
		}
		else {
			return determineImplicitElementCollectionTableName(
					ownerEntityName,
					ownerJpaEntityName,
					ownerEntityTable,
					propertyName
			);
		}
	}

	@Override
	public String determineLogicalEntityAssociationJoinTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyName) {
		if ( tableName != null ) {
			return tableName;
		}
		else {
			return determineImplicitEntityAssociationJoinTableName(
					ownerEntityName,
					ownerJpaEntityName,
					ownerEntityTable,
					associatedEntityName,
					associatedJpaEntityName,
					associatedEntityTable,
					propertyName
			);
		}
	}
}
