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
 * see/use {@link ImplicitNamingStrategyLegacyJpaImpl}.
 *
 * @author Steve Ebersole
 */
public class ImplicitNamingStrategyJpaCompliantImpl implements ImplicitNamingStrategy, Serializable {
	public static final ImplicitNamingStrategy INSTANCE = new ImplicitNamingStrategyJpaCompliantImpl();

	public ImplicitNamingStrategyJpaCompliantImpl() {
	}

	@Override
	public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
		assert source != null;
		final String tableName = transformEntityName( source.getEntityNaming() );
		if ( tableName == null ) {
			throw new HibernateException( "Could not determine primary table name for entity: "
											+ source.getEntityNaming().getClassName() );
		}
		return toIdentifier( tableName, source.getBuildingContext() );
	}

	protected String transformEntityName(EntityNaming entityNaming) {
		return isNotEmpty( entityNaming.getJpaEntityName() )
				// prefer the JPA entity name, if specified
				? entityNaming.getJpaEntityName()
				// otherwise, use the unqualified Hibernate entity name
				: unqualify( entityNaming.getEntityName() );
	}


	/**
	 * JPA states we should use the following as default:
	 * <blockquote>The concatenated names of the two associated primary entity
	 * tables (owning side first), separated by an underscore.</blockquote>
	 * That is:
	 * <pre>{OWNING SIDE PRIMARY TABLE NAME}_{NON-OWNING SIDE PRIMARY TABLE NAME}</pre>
	 */
	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		final String name = source.getOwningPhysicalTableName()
				+ '_'
				+ source.getNonOwningPhysicalTableName();
		return toIdentifier( name, source.getBuildingContext() );
	}

	/**
	 * JPA states we should use the following as default:
	 * <blockquote>The concatenation of the name of the containing entity and the
	 * name of the collection attribute, separated by an underscore.</blockquote>
	 * That is, if owning entity has a JPA entity name:
	 * <pre>{OWNER JPA ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}</pre>
	 * otherwise:
	 * <pre>{OWNER ENTITY NAME}_{COLLECTION ATTRIBUTE NAME}</pre>
	 */
	@Override
	public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
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

	/**
	 * JPA states we should use the following as default:
	 * <blockquote>The property or field name</blockquote>
	 * That is, the unqualified attribute path.
	 */
	@Override
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		return toIdentifier( transformAttributePath( source.getAttributePath() ), source.getBuildingContext() );
	}

	/**
	 * JPA states we should use the following as default:
	 * <ul>
	 * <li>If there is a "referencing relationship property":
	 *     <blockquote>The concatenation of the following: the name of the referencing
	 *     relationship property or field of the referencing entity or embeddable class;
	 *     {@code _}; the name of the referenced primary key column.</blockquote>
	 * <li>If there is no such "referencing relationship property",
	 *     or if the association is an element collection:
	 *     <blockquote>The concatenation of the following: the name of the entity;
	 *     {@code _}; the name of the referenced primary key column</blockquote>
	 * </ul>
	 */
	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		// TODO: we need to better account for "referencing relationship property"
		final String referencingPropertyOrEntity =
				source.getNature() == ELEMENT_COLLECTION || source.getAttributePath() == null
						? transformEntityName( source.getEntityNaming() )
						: transformAttributePath( source.getAttributePath() );
		final String referencedColumnName = source.getReferencedColumnName().getText();
		final String name = referencingPropertyOrEntity + '_' + referencedColumnName;
		return toIdentifier( name, source.getBuildingContext() );
	}

	/**
	 * JPA states we should use the following as default:
	 * <blockquote>the same name as the primary key column [of the referenced table]</blockquote>
	 */
	@Override
	public Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source) {
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
