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
@Deprecated
public class LegacyJpaNamingStrategyDelegate extends LegacyNamingStrategyDelegateAdapter {

	LegacyJpaNamingStrategyDelegate(LegacyNamingStrategyDelegate.LegacyNamingStrategyDelegateContext context) {
		super( context );
	}

	@Override
	public String determineImplicitPrimaryTableName(String entityName, String jpaEntityName) {
		// jpaEntityname is being passed here in order to not cause a regression. See HHH-4312.
		return getNamingStrategy().classToTableName( jpaEntityName );
	}

	@Override
	public String determineImplicitElementCollectionTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyPath) {
		return getNamingStrategy().collectionTableName(
				ownerEntityName,
				StringHelper.unqualifyEntityName( ownerEntityName ),
				null,
				null,
				propertyPath
		);
	}

	@Override
	public String determineImplicitElementCollectionJoinColumnName(
			String ownerEntityName, String ownerJpaEntityName, String ownerEntityTable, String referencedColumnName, String propertyPath) {
		return getNamingStrategy().foreignKeyColumnName(
				propertyPath,
				ownerEntityName,
				StringHelper.unqualifyEntityName( ownerEntityName ),
				referencedColumnName
		);
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
		return getNamingStrategy().collectionTableName(
				ownerEntityName,
				ownerEntityTable,
				associatedEntityName,
				associatedEntityTable,
				propertyPath
		);
	}

	@Override
	public String determineImplicitEntityAssociationJoinColumnName(
			String propertyEntityName, String propertyJpaEntityName, String propertyTableName, String referencedColumnName, String propertyPath) {
		return getNamingStrategy().foreignKeyColumnName(
				propertyPath,
				propertyEntityName,
				propertyTableName,
				referencedColumnName
		);
	}

	@Override
	public String determineLogicalElementCollectionTableName(
			String tableName,
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyName) {
		return getNamingStrategy().logicalCollectionTableName(
				tableName,
				ownerEntityName == null ? null : StringHelper.unqualifyEntityName( ownerEntityName ),
				null,
				propertyName
		);
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
		return getNamingStrategy().logicalCollectionTableName(
				tableName,
				ownerEntityTable,
				associatedEntityTable,
				propertyName
		);
	}
}
