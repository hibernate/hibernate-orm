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

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Gail Badner
 */
public class JpaNamingStrategyDelegate extends NamingStrategyDelegateAdapter {

	@Override
	public String determineImplicitPrimaryTableName(String entityName, String jpaEntityName) {
		return StringHelper.unqualify( determineEntityNameToUse( entityName, jpaEntityName ) );
	}

	@Override
	public String determineImplicitElementCollectionTableName(
			String ownerEntityName,
			String ownerJpaEntityName,
			String ownerEntityTable,
			String propertyPath) {
		// JPA states we should use the following as default:
		//      "The concatenation of the name of the containing entity and the name of the
		//       collection attribute, separated by an underscore.
		// aka:
		//     if owning entity has a JPA entity name: {OWNER JPA ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}
		//     otherwise: {OWNER ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}
		return determineEntityNameToUse( ownerEntityName, ownerJpaEntityName )
				+ '_'
				+ StringHelper.unqualify( propertyPath );
	}

	@Override
	public String determineImplicitElementCollectionJoinColumnName(
			String ownerEntityName, String ownerJpaEntityName, String ownerEntityTable, String referencedColumnName, String propertyPath) {
		// JPA states we should use the following as default:
		//     "The concatenation of the following: the name of the entity; "_"; the name of the
		//      referenced primary key column"
		return determineEntityNameToUse( ownerEntityName, ownerJpaEntityName )
				+ '_'
				+ referencedColumnName;
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
		// JPA states we should use the following as default:
		//		"The concatenated names of the two associated primary entity tables (owning side
		//		first), separated by an underscore."
		// aka:
		// 		{OWNING SIDE PRIMARY TABLE NAME}_{NON-OWNING SIDE PRIMARY TABLE NAME}

		return  ownerEntityTable
				+ '_'
				+ associatedEntityTable;
	}

	@Override
	public String determineImplicitEntityAssociationJoinColumnName(
			String propertyEntityName, String propertyJpaEntityName, String propertyTableName, String referencedColumnName, String referencingPropertyName) {
		// JPA states we should use the following as default:
		//      "The concatenation of the following: the name of the referencing relationship
		//      property or field of the referencing entity or embeddable class; "_"; the name
		//      of the referenced primary key column. If there is no such referencing relationship
		//      property or field in the entity, or if the join is for an element collection, the
		//      join column name is formed as the concatenation of the following: the name of the
		//      entity; "_"; the name of the referenced primary key column
		// The part referring to an entity collection can be disregarded here since, determination of
		// an element collection foreign key column name is covered  by #entityAssociationJoinTableName().
		//
		// For a unidirectional association:
		//      {PROPERTY_ENTITY_NAME}_{REFERENCED_COLUMN_NAME}
		// For a bidirectional association:
		//      {REFERENCING_PROPERTY_NAME}_{REFERENCED_COLUMN_NAME}
		final String header;
		if ( referencingPropertyName == null ) {
			// This is a unidirectional association.
			header = determineEntityNameToUse( propertyEntityName, propertyJpaEntityName );
		}
		else {
			// This is a bidirectional association.
			header = StringHelper.unqualify( referencingPropertyName );
		}
		if ( header == null ) {
			throw new AssertionFailure( "propertyJpaEntityName and referencingPropertyName cannot both be empty." );
		}
		return toPhysicalColumnName( header + "_" + referencedColumnName );
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

	private String determineEntityNameToUse(String entityName, String jpaEntityName) {
		if ( StringHelper.isNotEmpty( jpaEntityName ) ) {
			// prefer the JPA entity name, if specified...
			return jpaEntityName;
		}
		else {
			// otherwise, use the Hibernate entity name
			return StringHelper.unqualifyEntityName( entityName );
		}
	}
}
