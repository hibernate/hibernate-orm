/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * Implementation of the {@link ImplicitNamingStrategy} contract, generally
 * preferring to conform to JPA standards.
 * <p>
 * For the legacy JPA-based naming standards initially implemented by Hibernate,
 * see/use {@link ImplicitNamingStrategyLegacyJpaImpl}
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyJpaCompliantImpl implements ImplicitNamingStrategy, Serializable {
	public static final ImplicitNamingStrategy INSTANCE = new ImplicitNamingStrategyJpaCompliantImpl();

	public ImplicitNamingStrategyJpaCompliantImpl() {
	}

	@Override
	public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
		if ( source == null ) {
			// should never happen, but to be defensive...
			throw new HibernateException( "Entity naming information was not provided." );
		}

		final String tableName = transformEntityName( source.getEntityNaming() );

		if ( tableName == null ) {
			// todo : add info to error message - but how to know what to write since we failed to interpret the naming source
			throw new HibernateException( "Could not determine primary table name for entity" );
		}

		return toIdentifier( tableName, source.getBuildingContext() );
	}

	protected String transformEntityName(EntityNaming entityNaming) {
		// prefer the JPA entity name, if specified...
		if ( isNotEmpty( entityNaming.getJpaEntityName() ) ) {
			return entityNaming.getJpaEntityName();
		}
		else {
			// otherwise, use the Hibernate entity name
			return unqualify( entityNaming.getEntityName() );
		}
	}


	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		// JPA states we should use the following as default:
		//		"The concatenated names of the two associated primary entity tables (owning side
		//		first), separated by an underscore."
		// aka:
		// 		{OWNING SIDE PRIMARY TABLE NAME}_{NON-OWNING SIDE PRIMARY TABLE NAME}
		final String name = source.getOwningPhysicalTableName()
				+ '_'
				+ source.getNonOwningPhysicalTableName();
		return toIdentifier( name, source.getBuildingContext() );
	}


	@Override
	public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
		// JPA states we should use the following as default:
		//      "The concatenation of the name of the containing entity and the name of the
		//       collection attribute, separated by an underscore.
		// aka:
		//     if owning entity has a JPA entity name: {OWNER JPA ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}
		//     otherwise: {OWNER ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}
		final String name = transformEntityName( source.getOwningEntityNaming() )
				+ '_'
				+ transformAttributePath( source.getOwningAttributePath() );
		return toIdentifier( name, source.getBuildingContext() );
	}

	@Override
	public Identifier determineIdentifierColumnName(ImplicitIdentifierColumnNameSource source) {
		// JPA states the implicit column name should be the attribute name
		return toIdentifier(
				transformAttributePath( source.getIdentifierAttributePath() ),
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier determineDiscriminatorColumnName(ImplicitDiscriminatorColumnNameSource source) {
		return toIdentifier(
				source.getBuildingContext().getEffectiveDefaults().getDefaultDiscriminatorColumnName(),
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier determineTenantIdColumnName(ImplicitTenantIdColumnNameSource source) {
		return toIdentifier(
				source.getBuildingContext().getEffectiveDefaults().getDefaultTenantIdColumnName(),
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		// JPA states we should use the following as default:
		//     "The property or field name"
		// aka:
		//     The unqualified attribute path.
		return toIdentifier( transformAttributePath( source.getAttributePath() ), source.getBuildingContext() );
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		// JPA states we should use the following as default:
		//
		//	(1) if there is a "referencing relationship property":
		//		"The concatenation of the following: the name of the referencing relationship
		// 			property or field of the referencing entity or embeddable class; "_"; the
		// 			name of the referenced primary key column."
		//
		//	(2) if there is no such "referencing relationship property", or if the association is
		// 			an element collection:
		//     "The concatenation of the following: the name of the entity; "_"; the name of the
		// 			referenced primary key column"

		// todo : we need to better account for "referencing relationship property"

		final String referencedColumnName = source.getReferencedColumnName().getText();

		final String name;
		if ( source.getNature() == ELEMENT_COLLECTION
				|| source.getAttributePath() == null ) {
			name = transformEntityName( source.getEntityNaming() )
					+ '_' + referencedColumnName;
		}
		else {
			name = transformAttributePath( source.getAttributePath() )
					+ '_' + referencedColumnName;
		}

		return toIdentifier( name, source.getBuildingContext() );
	}

	@Override
	public Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source) {
		// JPA states we should use the following as default:
		// 		"the same name as the primary key column [of the referenced table]"
		return source.getReferencedPrimaryKeyColumnName();
	}

	@Override
	public Identifier determineAnyDiscriminatorColumnName(ImplicitAnyDiscriminatorColumnNameSource source) {
		final MetadataBuildingContext buildingContext = source.getBuildingContext();
		return toIdentifier(
				transformAttributePath( source.getAttributePath() )
						+ "_" + buildingContext.getEffectiveDefaults().getDefaultDiscriminatorColumnName(),
				buildingContext
		);
	}

	@Override
	public Identifier determineAnyKeyColumnName(ImplicitAnyKeyColumnNameSource source) {
		final MetadataBuildingContext buildingContext = source.getBuildingContext();
		return toIdentifier(
				transformAttributePath( source.getAttributePath() )
						+ "_" + buildingContext.getEffectiveDefaults().getDefaultIdColumnName(),
				buildingContext
		);
	}


	@Override
	public Identifier determineMapKeyColumnName(ImplicitMapKeyColumnNameSource source) {
		return toIdentifier(
				transformAttributePath( source.getPluralAttributePath() ) + "_KEY",
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier determineListIndexColumnName(ImplicitIndexColumnNameSource source) {
		return toIdentifier(
				transformAttributePath( source.getPluralAttributePath() ) + "_ORDER",
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
		final Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
		final MetadataBuildingContext buildingContext = source.getBuildingContext();
		return userProvidedIdentifier != null ? userProvidedIdentifier : toIdentifier(
				NamingHelper.withCharset( buildingContext.getBuildingOptions().getSchemaCharset() )
						.generateHashedFkName( "FK", source.getTableName(),
								source.getReferencedTableName(), source.getColumnNames() ),
				buildingContext
		);
	}

	@Override
	public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
		final Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
		final MetadataBuildingContext buildingContext = source.getBuildingContext();
		return userProvidedIdentifier != null ? userProvidedIdentifier : toIdentifier(
				NamingHelper.withCharset( buildingContext.getBuildingOptions().getSchemaCharset() )
						.generateHashedConstraintName( "UK", source.getTableName(), source.getColumnNames() ),
				buildingContext
		);
	}

	@Override
	public Identifier determineIndexName(ImplicitIndexNameSource source) {
		final Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
		final MetadataBuildingContext buildingContext = source.getBuildingContext();
		return userProvidedIdentifier != null ? userProvidedIdentifier : toIdentifier(
				NamingHelper.withCharset( buildingContext.getBuildingOptions().getSchemaCharset() )
						.generateHashedConstraintName( "IDX", source.getTableName(), source.getColumnNames() ),
				buildingContext
		);
	}

	/**
	 * For JPA standards we typically need the unqualified name.  However, a more usable
	 * impl tends to use the whole path.  This method provides an easy hook for subclasses
	 * to accomplish that
	 *
	 * @param attributePath The attribute path
	 *
	 * @return The extracted name
	 */
	protected String transformAttributePath(AttributePath attributePath) {
		return attributePath.getProperty();
	}

	/**
	 * Easy hook to build an Identifier using the keyword safe IdentifierHelper.
	 *
	 * @param stringForm The String form of the name
	 * @param buildingContext Access to the IdentifierHelper
	 *
	 * @return The identifier
	 */
	protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
		return buildingContext.getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment()
				.getIdentifierHelper()
				.toIdentifier( stringForm );
	}
}
