/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.View;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.InLineView;
import org.hibernate.boot.models.bind.internal.PhysicalTable;
import org.hibernate.boot.models.bind.internal.PhysicalView;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.internal.sources.TableSource;
import org.hibernate.boot.models.bind.internal.UnionTable;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.SecondaryTable;

import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotationUsages;

/// Creates and registers table references used by mapping-model binders.
///
/// Table binding bridges source annotations, implicit naming, physical naming,
/// and `org.hibernate.mapping.Table` creation.  It handles primary tables,
/// secondary tables, collection tables, association join tables, inheritance
/// tables, subselects, and denormalized union-subclass tables.
///
/// Only the table shell is created here.  Keys that depend on identifiers are
/// deliberately deferred to [TableKeyBinder], and physical foreign-key
/// constraints are deferred again to [ForeignKeyBinder].
///
/// @since 9.0
/// @author Steve Ebersole
public class TableBinder {
	private final ModelBinders modelBinders;

	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;

	private final JdbcEnvironment jdbcEnvironment;

	public TableBinder(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			ModelBinders modelBinders) {
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
		this.modelBinders = modelBinders;

		this.implicitNamingStrategy = bindingContext.getImplicitNamingStrategy();
		this.physicalNamingStrategy = bindingContext.getPhysicalNamingStrategy();

		this.jdbcEnvironment = bindingContext.getServiceRegistry().getService( JdbcEnvironment.class );
	}

	public TableReference bindPrimaryTable(EntityTypeMetadata type, EntityHierarchy.HierarchyRelation hierarchyRelation) {
		final ClassDetails typeClassDetails = type.getClassDetails();
		final jakarta.persistence.Table tableAnn = typeClassDetails.getDirectAnnotationUsage( jakarta.persistence.Table.class );
		final JoinTable joinTableAnn = typeClassDetails.getDirectAnnotationUsage( JoinTable.class );
		final Subselect subselectAnn = typeClassDetails.getDirectAnnotationUsage( Subselect.class );
		final View viewAnn = typeClassDetails.getDirectAnnotationUsage( View.class );

		if ( tableAnn != null && joinTableAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @JoinTable on " + typeClassDetails.getName() );
		}
		if ( tableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
		}
		if ( joinTableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @JoinTable and @Subselect on " + typeClassDetails.getName() );
		}
		if ( subselectAnn != null && viewAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Subselect and @View on " + typeClassDetails.getName() );
		}

		final TableReference tableReference;

		if ( type.getHierarchy().getInheritanceType() == InheritanceType.TABLE_PER_CLASS ) {
			assert subselectAnn == null;

			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = bindPhysicalTable( type, TableSource.from( tableAnn ), true, viewAnn );
			}
			else {
				tableReference = bindUnionTable( type, TableSource.from( tableAnn ) );
			}
		}
		else if ( type.getHierarchy().getInheritanceType() == InheritanceType.SINGLE_TABLE ) {
			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = normalTableDetermination( type, subselectAnn, TableSource.from( tableAnn ), viewAnn );
			}
			else {
				tableReference = null;
			}
		}
		else {
			tableReference = normalTableDetermination( type, subselectAnn, TableSource.from( joinTableAnn ), viewAnn );
		}

		if ( tableReference != null ) {
			bindingState.addTable( type, tableReference );
			applyRowId( tableReference.binding(), type );

			final PrimaryKey primaryKey = new PrimaryKey( tableReference.binding() );
			tableReference.binding().setPrimaryKey( primaryKey );
		}

		return tableReference;
	}

	private TableReference normalTableDetermination(
			EntityTypeMetadata type,
			Subselect subselectAnn,
			TableSource tableSource,
			View viewAnn) {
		final TableReference tableReference;
		if ( subselectAnn != null ) {
			tableReference = bindVirtualTable( type, subselectAnn );
		}
		else {
			// either an explicit or implicit @Table
			tableReference = bindPhysicalTable( type, tableSource, true, viewAnn );
		}
		return tableReference;
	}

	private TableReference bindUnionTable(
			EntityTypeMetadata type,
			TableSource tableSource) {
		assert type.getSuperType() != null;

		final TableReference superTypeTable = bindingState.getTableByOwner( type.getSuperType() );
		final Table unionBaseTable = superTypeTable.binding();

		final Identifier logicalName = determineLogicalName( type, tableSource );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final DenormalizedTable binding = (DenormalizedTable) bindingState.getMetadataBuildingContext().getMetadataCollector().addDenormalizedTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				type.isAbstract(),
				null,
				unionBaseTable,
				bindingState.getMetadataBuildingContext()
		);
		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyCheckConstraints( binding, tableSource );

		return new UnionTable( logicalName, superTypeTable, binding, !type.hasSubTypes() );
	}

	public List<org.hibernate.boot.models.bind.internal.SecondaryTable> bindSecondaryTables(EntityTypeBinder entityBinder) {
		final ClassDetails typeClassDetails = entityBinder.getManagedType().getClassDetails();

		final List<SecondaryTable> secondaryTableAnns = Arrays.asList( typeClassDetails.getRepeatedAnnotationUsages(
				SecondaryTable.class,
				bindingContext.getBootstrapContext().getModelsContext()
		) );
		final List<org.hibernate.boot.models.bind.internal.SecondaryTable> result = new ArrayList<>( secondaryTableAnns.size() );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final SecondaryRow secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.name(),
					"table",
					bindingContext.getBootstrapContext().getModelsContext()
			);
			final org.hibernate.boot.models.bind.internal.SecondaryTable binding = bindSecondaryTable( entityBinder, secondaryTableAnn, secondaryRowAnn );
			result.add( binding );
			bindingState.addSecondaryTable( binding );
		} );
		return result;
	}

	private InLineView bindVirtualTable(EntityTypeMetadata type, Subselect subselectAnn) {
		final Identifier logicalName = implicitNamingStrategy.determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);

		return new InLineView(
				logicalName,
				bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
						null,
						null,
						logicalName.getCanonicalName(),
						subselectAnn.value(),
						true,
						bindingState.getMetadataBuildingContext(),
						false
				)
		);
	}

	private TableReference bindPhysicalTable(
			EntityTypeMetadata type,
			TableSource tableSource,
			boolean isPrimary) {
		return bindPhysicalTable( type, tableSource, isPrimary, null );
	}

	private TableReference bindPhysicalTable(
			EntityTypeMetadata type,
			TableSource tableSource,
			boolean isPrimary,
			View viewAnn) {
		if ( tableSource != null ) {
			return bindExplicitPhysicalTable( type, tableSource, isPrimary, viewAnn );
		}
		else {
			return bindImplicitPhysicalTable( type, isPrimary, viewAnn );
		}
	}

	private TableReference bindImplicitPhysicalTable(EntityTypeMetadata type, boolean isPrimary, View viewAnn) {
		final Identifier logicalName = determineLogicalName( type, null );
		final Identifier logicalSchemaName = bindingOptions.getDefaultSchemaName();
		final Identifier logicalCatalogName = bindingOptions.getDefaultCatalogName();

		final Table binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				toCanonicalName( logicalSchemaName ),
				toCanonicalName( logicalCatalogName ),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext(),
				false
		);

		applyComment( binding, null );
		applyHibernateChecks( binding, type );
		applyView( binding, viewAnn );

		return createPhysicalTableReference(
				viewAnn,
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
				binding
		);
	}

	private Identifier determineLogicalName(EntityTypeMetadata type, TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment );
			}
		}

		return implicitNamingStrategy.determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);
	}

	private TableReference bindExplicitPhysicalTable(
			EntityTypeMetadata type,
			TableSource tableSource,
			boolean isPrimary,
			View viewAnn) {
		final Identifier logicalName = determineLogicalName( type, tableSource );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext(),
				false
		);

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyHibernateChecks( binding, type );
		applyView( binding, viewAnn );

		return createPhysicalTableReference(
				viewAnn,
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				logicalCatalogName == null ? null : physicalNamingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				logicalSchemaName == null ? null : physicalNamingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
				binding
		);
	}

	private static void applyView(Table binding, View viewAnn) {
		if ( viewAnn != null ) {
			binding.setViewQuery( viewAnn.query() );
		}
	}

	private static TableReference createPhysicalTableReference(
			View viewAnn,
			Identifier logicalName,
			Identifier logicalCatalogName,
			Identifier logicalSchemaName,
			Identifier physicalName,
			Identifier physicalCatalogName,
			Identifier physicalSchemaName,
			Table binding) {
		if ( viewAnn != null ) {
			return new PhysicalView(
					logicalName,
					logicalCatalogName,
					logicalSchemaName,
					physicalName,
					physicalCatalogName,
					physicalSchemaName,
					binding
			);
		}
		return new PhysicalTable(
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalName,
				physicalCatalogName,
				physicalSchemaName,
				binding
		);
	}

	public PhysicalTable bindCollectionTable(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			CollectionTable collectionTable) {
		final TableSource tableSource = TableSource.from( collectionTable );
		final Identifier logicalName = determineCollectionTableLogicalName(
				ownerType,
				owningTable,
				attributeName,
				tableSource
		);
		return bindPhysicalTable( logicalName, tableSource, false );
	}

	public PhysicalTable bindOwnedTable(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			JoinTable joinTable) {
		final TableSource tableSource = TableSource.from( joinTable );
		final Identifier logicalName = determineCollectionTableLogicalName(
				ownerType,
				owningTable,
				attributeName,
				tableSource
		);
		return bindPhysicalTable( logicalName, tableSource, false );
	}

	public PhysicalTable bindAssociationTable(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			EntityNaming targetType,
			Table targetTable,
			JoinTable joinTable) {
		final TableSource tableSource = TableSource.from( joinTable );
		final Identifier logicalName = determineAssociationTableLogicalName(
				ownerType,
				owningTable,
				attributeName,
				targetType,
				targetTable,
				tableSource
		);
		return bindPhysicalTable( logicalName, tableSource, false );
	}

	private PhysicalTable bindPhysicalTable(
			Identifier logicalName,
			TableSource tableSource,
			boolean isAbstract) {
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				toCanonicalName( logicalSchemaName ),
				toCanonicalName( logicalCatalogName ),
				logicalName.getCanonicalName(),
				null,
				isAbstract,
				bindingState.getMetadataBuildingContext(),
				false
		);

			applyComment( binding, tableSource );
			applyOptions( binding, tableSource );
			applyCheckConstraints( binding, tableSource );

			return new PhysicalTable(
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
				binding
		);
	}

	private Identifier determineCollectionTableLogicalName(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment );
			}
		}

		return implicitNamingStrategy.determineCollectionTableName(
				new ImplicitCollectionTableNameSource() {
					@Override
					public Identifier getOwningPhysicalTableName() {
						return Identifier.toIdentifier( owningTable.getName() );
					}

					@Override
					public EntityNaming getOwningEntityNaming() {
						return ownerType;
					}

					@Override
					public AttributePath getOwningAttributePath() {
						return AttributePath.parse( attributeName );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);
	}

	private Identifier determineAssociationTableLogicalName(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			EntityNaming targetType,
			Table targetTable,
			TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment );
			}
		}

		return implicitNamingStrategy.determineJoinTableName(
				new ImplicitJoinTableNameSource() {
					@Override
					public String getOwningPhysicalTableName() {
						return owningTable.getName();
					}

					@Override
					public EntityNaming getOwningEntityNaming() {
						return ownerType;
					}

					@Override
					public String getNonOwningPhysicalTableName() {
						return targetTable.getName();
					}

					@Override
					public EntityNaming getNonOwningEntityNaming() {
						return targetType;
					}

					@Override
					public AttributePath getAssociationOwningAttributePath() {
						return AttributePath.parse( attributeName );
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				}
		);
	}

	private org.hibernate.boot.models.bind.internal.SecondaryTable bindSecondaryTable(
			EntityTypeBinder entityBinder,
			SecondaryTable secondaryTableAnn,
			SecondaryRow secondaryRowAnn) {
		final TableSource tableSource = TableSource.from( secondaryTableAnn );
		final Identifier logicalName = determineLogicalName( entityBinder.getManagedType(), tableSource );
		final Identifier schemaName = resolveDatabaseIdentifier(
				tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier catalogName = resolveDatabaseIdentifier(
				tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				toCanonicalName( schemaName ),
				toCanonicalName( catalogName ),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext(),
				false
		);

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyCheckConstraints( binding, tableSource );

		final Join join = new Join();
		join.setTable( binding );
		final boolean optional = secondaryRowAnn == null || secondaryRowAnn.optional();
		final boolean owned = secondaryRowAnn == null || secondaryRowAnn.owned();
		join.setOptional( optional );
		join.setInverse( !owned );
		join.setPersistentClass( entityBinder.getTypeBinding() );
		entityBinder.getTypeBinding().addJoin( join );

		return new org.hibernate.boot.models.bind.internal.SecondaryTable(
				logicalName,
				catalogName,
				schemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment ),
				optional,
				owned,
				ForeignKeySource.from( secondaryTableAnn ),
				binding
		);
	}

	private String toCanonicalName(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return name.getCanonicalName();
	}

	private Identifier resolveDatabaseIdentifier(
			String explicit,
			Identifier fallback,
			QuotedIdentifierTarget target) {
		if ( StringHelper.isNotEmpty( explicit ) ) {
			return BindingHelper.toIdentifier( explicit, target, bindingOptions, jdbcEnvironment );
		}

		if ( fallback != null ) {
			return fallback;
		}

		return null;
	}


	private void applyComment(Table table, TableSource tableSource) {
		if ( tableSource != null ) {
			final String comment = tableSource.comment();
			if ( StringHelper.isNotEmpty( comment ) ) {
				table.setComment( comment );
			}
		}
	}

	private void applyOptions(Table table, TableSource tableSource) {
		if ( tableSource != null ) {
			final String options = tableSource.options();
			if ( StringHelper.isNotEmpty( options ) ) {
				table.setOptions( options );
			}
		}
	}

	private void applyCheckConstraints(Table table, TableSource tableSource) {
		if ( tableSource == null ) {
			return;
		}

		final jakarta.persistence.CheckConstraint[] checkConstraints = tableSource.checkConstraints();
		if ( checkConstraints == null ) {
			return;
		}

		for ( jakarta.persistence.CheckConstraint checkConstraint : checkConstraints ) {
			if ( StringHelper.isEmpty( checkConstraint.constraint() ) ) {
				continue;
			}
			table.addCheck( new org.hibernate.mapping.CheckConstraint(
					StringHelper.nullIfEmpty( checkConstraint.name() ),
					checkConstraint.constraint(),
					StringHelper.nullIfEmpty( checkConstraint.options() )
			) );
		}
	}

	@SuppressWarnings("removal")
	private void applyHibernateChecks(Table table, EntityTypeMetadata type) {
		final Check[] checks = getOverridableAnnotationUsages(
				type.getClassDetails(),
				Check.class,
				bindingState.getDatabase().getDialect(),
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( Check check : checks ) {
			if ( StringHelper.isNotEmpty( check.constraints() ) ) {
				table.addCheck( new org.hibernate.mapping.CheckConstraint(
						StringHelper.nullIfEmpty( check.name() ),
						check.constraints(),
						null
				) );
			}
		}
	}

	private void applyRowId(Table table, EntityTypeMetadata type) {
		final RowId rowId = type.getClassDetails().getDirectAnnotationUsage( RowId.class );
		if ( rowId != null && StringHelper.isNotEmpty( rowId.value() ) ) {
			table.setRowId( rowId.value() );
		}
	}
}
