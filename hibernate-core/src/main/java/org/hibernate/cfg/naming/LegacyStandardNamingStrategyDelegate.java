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
public class LegacyStandardNamingStrategyDelegate extends AbstractLegacyNamingStrategyDelegate {

	LegacyStandardNamingStrategyDelegate(LegacyNamingStrategyDelegate.LegacyNamingStrategyDelegateContext context) {
		super( context );
	}

	@Override
	public String determineElementCollectionTableLogicalName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyNamePath) {
		return getNamingStrategy().collectionTableName(
				ownerEntityName,
				StringHelper.unqualifyEntityName( ownerEntityName ),
				null,
				null,
				propertyNamePath
		);
	}

	@Override
	public String determineElementCollectionForeignKeyColumnName(
			String propertyName,
			String propertyEntityName,
			String propertyJpaEntityName,
			String propertyTableName,
			String referencedColumnName) {
		return getNamingStrategy().foreignKeyColumnName(
				propertyName,
				propertyEntityName,
				StringHelper.unqualifyEntityName( propertyEntityName ),
				referencedColumnName
		);
	}

	@Override
	public String determineEntityAssociationJoinTableLogicalName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String associatedEntityName,
			String associatedJpaEntityName,
			String associatedEntityTable,
			String propertyNamePath) {
		return getNamingStrategy().collectionTableName(
				ownerEntityName,
				ownerEntityTable,
				associatedEntityName,
				associatedEntityTable,
				propertyNamePath
		);
	}

	@Override
	public String determineEntityAssociationForeignKeyColumnName(
			String propertyName,
			String propertyEntityName,
			String propertyJpaEntityName,
			String propertyTableName,
			String referencedColumnName) {
		return getNamingStrategy().foreignKeyColumnName(
				propertyName,
				propertyEntityName,
				propertyTableName,
				referencedColumnName
		);
	}

	@Override
	public String logicalElementCollectionTableName(
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
	public String logicalEntityAssociationJoinTableName(
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
