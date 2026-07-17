/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.View;
import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.mapping.internal.context.BindingHelper;
import org.hibernate.boot.mapping.internal.materialize.IndexMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PrimaryTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedIndex;
import org.hibernate.boot.mapping.internal.materialize.ResolvedUniqueKey;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.relational.InLineView;
import org.hibernate.boot.mapping.internal.relational.PhysicalTable;
import org.hibernate.boot.mapping.internal.relational.PhysicalView;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.TableSource;
import org.hibernate.boot.mapping.internal.relational.UnionTable;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

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
	private final PrimaryTableKeyMappingMaterializer primaryTableKeyMappingMaterializer;

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
		this.primaryTableKeyMappingMaterializer = new PrimaryTableKeyMappingMaterializer(
				bindingState.getMetadataBuildingContext()
		);
		this.modelBinders = modelBinders;

		this.implicitNamingStrategy = bindingContext.getImplicitNamingStrategy();
		this.physicalNamingStrategy = bindingContext.getPhysicalNamingStrategy();

		this.jdbcEnvironment = bindingContext.getServiceRegistry().getService( JdbcEnvironment.class );
	}

	public TableReference bindPrimaryTable(EntityTypeBinder entityBinder) {
		final EntityTypeMetadata type = entityBinder.getManagedType();
		final EntityHierarchy.HierarchyRelation hierarchyRelation = entityBinder.getHierarchyRelation();
		final ClassDetails typeClassDetails = type.getClassDetails();
		final jakarta.persistence.Table tableAnn = typeClassDetails.getDirectAnnotationUsage( jakarta.persistence.Table.class );
		final JoinTable joinTableAnn = typeClassDetails.getDirectAnnotationUsage( JoinTable.class );
		final Subselect subselectAnn = typeClassDetails.getDirectAnnotationUsage( Subselect.class );
		final View viewAnn = typeClassDetails.getDirectAnnotationUsage( View.class );

		if ( tableAnn != null && joinTableAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @JoinTable on " + typeClassDetails.getName() );
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
				tableReference = bindUnionTable( entityBinder, TableSource.from( tableAnn ) );
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
			tableReference = normalTableDetermination( type, subselectAnn, TableSource.from( tableAnn ), viewAnn );
		}

		if ( tableReference != null ) {
			bindingState.addTable( type, tableReference );
			applyRowId( tableReference.binding(), type );

			primaryTableKeyMappingMaterializer.initializePrimaryKey(
					primaryTableKeyMappingMaterializer.resolvePrimaryKey(
							entityBinder.getTypeBinding(),
							tableReference.binding()
					)
			);
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
			tableReference = bindVirtualTable( type, subselectAnn, tableSource );
		}
		else {
			// either an explicit or implicit @Table
			tableReference = bindPhysicalTable( type, tableSource, true, viewAnn );
		}
		return tableReference;
	}

	private TableReference bindUnionTable(
			EntityTypeBinder entityBinder,
			TableSource tableSource) {
		final EntityTypeMetadata type = entityBinder.getManagedType();
		final EntityTypeBinder superEntityBinder = entityBinder.getSuperEntityBinder();
		if ( superEntityBinder == null ) {
			throw new MappingException( "Unable to resolve super entity table for table-per-class entity - "
					+ type.getEntityName() );
		}

		final TableReference superTypeTable = bindingState.getTableByOwner( superEntityBinder.getManagedType() );
		if ( superTypeTable == null ) {
			throw new MappingException( "Unable to resolve super entity table for table-per-class entity - "
					+ type.getEntityName() + " : " + superEntityBinder.getManagedType().getEntityName() );
		}
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
		final boolean explicitTableName = tableSource != null && tableSource.nonEmptyName() != null;

		final DenormalizedTable binding = bindingState.createDenormalizedTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				explicitTableName ? logicalName.render() : logicalName.getText(),
				type.isAbstract(),
				null,
				unionBaseTable
		);
		registerLegacyLogicalTableName( logicalName, binding );
		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyUniqueConstraints( binding, tableSource );
		applyIndexes( binding, tableSource );

		return new UnionTable( logicalName, superTypeTable, binding, !type.hasSubTypes() );
	}

	public List<org.hibernate.boot.mapping.internal.relational.SecondaryTable> bindSecondaryTables(EntityTypeBinder entityBinder) {
		final ClassDetails typeClassDetails = entityBinder.getManagedType().getClassDetails();

		final List<SecondaryTable> secondaryTableAnns = Arrays.asList( typeClassDetails.getRepeatedAnnotationUsages(
				SecondaryTable.class,
				bindingContext.getModelsContext()
		) );
		final List<org.hibernate.boot.mapping.internal.relational.SecondaryTable> result = new ArrayList<>( secondaryTableAnns.size() );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final SecondaryRow secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.name(),
					"table",
					bindingContext.getModelsContext()
			);
			final org.hibernate.boot.mapping.internal.relational.SecondaryTable binding = bindSecondaryTable( entityBinder, secondaryTableAnn, secondaryRowAnn );
			result.add( binding );
			bindingState.addSecondaryTable( binding );
		} );
		return result;
	}

	private InLineView bindVirtualTable(
			EntityTypeMetadata type,
			Subselect subselectAnn,
			TableSource tableSource) {
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

		final Table binding = bindingState.getOrCreateTable(
				explicitSchemaName( tableSource, logicalSchemaName ),
				explicitCatalogName( tableSource, logicalCatalogName ),
				nameForAddTable( logicalName ),
				subselectAnn.value(),
				true,
				tableSource != null && tableSource.nonEmptyName() != null
		);
		registerLegacyLogicalTableName( logicalName, binding );

		return new InLineView( logicalName, binding );
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

		final Table binding = bindingState.getOrCreateTable(
					toCanonicalName( logicalSchemaName ),
					toCanonicalName( logicalCatalogName ),
					nameForAddTable( logicalName ),
					null,
					type.isAbstract(),
				false
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, null );
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

		final var binding = bindingState.getOrCreateTable(
				explicitSchemaName( tableSource, logicalSchemaName ),
				explicitCatalogName( tableSource, logicalCatalogName ),
				nameForAddTable( logicalName ),
				null,
				type.isAbstract(),
				tableSource.nonEmptyName() != null
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyUniqueConstraints( binding, tableSource );
		applyIndexes( binding, tableSource );
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

	private void registerLegacyLogicalTableName(Identifier logicalName, Table table) {
		bindingState.getMetadataBuildingContext().getMetadataCollector().addTableNameBinding( logicalName, table );
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
		return registerTable( bindPhysicalTable( logicalName, tableSource, false ) );
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
		return registerTable( bindPhysicalTable( logicalName, tableSource, false ) );
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
		return registerTable( bindPhysicalTable( logicalName, tableSource, false ) );
	}

	public PhysicalTable bindAssociationTable(
			EntityTypeMetadata ownerType,
			Table owningTable,
			String attributeName,
			EntityNaming targetType,
			Table targetTable,
			CollectionTable collectionTable) {
		final TableSource tableSource = TableSource.from( collectionTable );
		final Identifier logicalName = determineAssociationTableLogicalName(
				ownerType,
				owningTable,
				attributeName,
				targetType,
				targetTable,
				tableSource
		);
		return registerTable( bindPhysicalTable( logicalName, tableSource, false ) );
	}

	private PhysicalTable registerTable(PhysicalTable table) {
		bindingState.addTableBinding( table );
		return table;
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

		final var binding = bindingState.getOrCreateTable(
					explicitSchemaName( tableSource, logicalSchemaName ),
					explicitCatalogName( tableSource, logicalCatalogName ),
					nameForAddTable( logicalName ),
					null,
					isAbstract,
				tableSource != null && tableSource.nonEmptyName() != null
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
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

	private String explicitSchemaName(TableSource tableSource, Identifier logicalSchemaName) {
		return tableSource != null && tableSource.schema() != null && !tableSource.schema().isEmpty()
				? tableSource.schema()
				: toCanonicalName( logicalSchemaName );
	}

	private String explicitCatalogName(TableSource tableSource, Identifier logicalCatalogName) {
		return tableSource != null && tableSource.catalog() != null && !tableSource.catalog().isEmpty()
				? tableSource.catalog()
				: toCanonicalName( logicalCatalogName );
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

	private org.hibernate.boot.mapping.internal.relational.SecondaryTable bindSecondaryTable(
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

		final var binding = bindingState.getOrCreateTable(
				explicitSchemaName( tableSource, schemaName ),
				explicitCatalogName( tableSource, catalogName ),
				nameForAddTable( logicalName ),
				null,
				false,
				tableSource.nonEmptyName() != null
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyUniqueConstraints( binding, tableSource );
		applyIndexes( binding, tableSource );

		final Join join = new Join();
		join.setTable( binding );
		final boolean optional = secondaryRowAnn == null || secondaryRowAnn.optional();
		final boolean owned = secondaryRowAnn == null || secondaryRowAnn.owned();
		join.setOptional( optional );
		join.setInverse( !owned );
		join.setPersistentClass( entityBinder.getTypeBinding() );
		entityBinder.getTypeBinding().addJoin( join );

		return new org.hibernate.boot.mapping.internal.relational.SecondaryTable(
				logicalName,
				catalogName,
				schemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment ),
				optional,
				owned,
				primaryKeyJoinColumns( secondaryTableAnn.pkJoinColumns() ),
				ForeignKeySource.from( secondaryTableAnn ),
				binding
		);
	}

	private List<JoinColumn> primaryKeyJoinColumns(PrimaryKeyJoinColumn[] primaryKeyJoinColumns) {
		if ( primaryKeyJoinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( primaryKeyJoinColumns.length );
		for ( PrimaryKeyJoinColumn primaryKeyJoinColumn : primaryKeyJoinColumns ) {
			result.add( JoinColumnJpaAnnotation.toJoinColumn(
					primaryKeyJoinColumn,
					bindingContext.getModelsContext()
			) );
		}
		return result;
	}

	private String toCanonicalName(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return name.getCanonicalName();
	}

	private String nameForAddTable(Identifier logicalName) {
		return logicalName.render();
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

	private void applyType(Table table, TableSource tableSource) {
		if ( tableSource != null ) {
			final String type = tableSource.type();
			if ( StringHelper.isNotEmpty( type ) ) {
				table.setType( type );
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

	private void applyUniqueConstraints(Table table, TableSource tableSource) {
		if ( tableSource == null || tableSource.uniqueConstraints() == null ) {
			return;
		}

		for ( jakarta.persistence.UniqueConstraint uniqueConstraint : tableSource.uniqueConstraints() ) {
			validateUniqueConstraintColumns( uniqueConstraint.columnNames(), table.getName() );
			final ArrayList<Column> uniqueKeyColumns = new ArrayList<>( uniqueConstraint.columnNames().length );
			for ( String columnName : uniqueConstraint.columnNames() ) {
				uniqueKeyColumns.add( createColumn( columnName ) );
			}
			UniqueKeyMappingMaterializer.materializeUniqueKey(
					ResolvedUniqueKey.explicit(
							table,
							uniqueKeyColumns,
							bindingState.getMetadataBuildingContext(),
							StringHelper.nullIfEmpty( uniqueConstraint.name() ),
							StringHelper.isNotEmpty( uniqueConstraint.name() ),
							uniqueConstraint.options(),
							null,
							"table-unique-constraint"
					)
			);
		}
	}

	private void validateUniqueConstraintColumns(String[] columnNames, String tableName) {
		if ( columnNames.length == 0 ) {
			throw new AnnotationException( "Unique constraint on table '" + tableName + "' did not specify columns" );
		}
		for ( String columnName : columnNames ) {
			if ( StringHelper.isEmpty( columnName ) ) {
				throw new AnnotationException(
						"Unique constraint on table '" + tableName + "' specified an empty column name"
				);
			}
		}
	}

	private void applyIndexes(Table table, TableSource tableSource) {
		if ( tableSource == null || tableSource.indexes() == null ) {
			return;
		}

		for ( jakarta.persistence.Index indexAnn : tableSource.indexes() ) {
			final List<String> parsed = parseColumnList( indexAnn.columnList() );
			if ( parsed.isEmpty() ) {
				continue;
			}
			final String[] columnExpressions = new String[parsed.size()];
			final String[] orderings = new String[parsed.size()];
			initializeColumns( columnExpressions, orderings, parsed );

			final Selectable[] selectables = selectables( columnExpressions );
			boolean hasFormula = false;
			for ( Selectable selectable : selectables ) {
				if ( selectable.isFormula() ) {
					hasFormula = true;
					break;
				}
			}

			if ( indexAnn.unique()
					&& !hasFormula
					&& jdbcEnvironment.getDialect().supportsUniqueConstraints()
					&& StringHelper.isEmpty( indexAnn.type() )
					&& StringHelper.isEmpty( indexAnn.using() ) ) {
				final ArrayList<Column> uniqueKeyColumns = new ArrayList<>( selectables.length );
				for ( Selectable selectable : selectables ) {
					uniqueKeyColumns.add( (Column) selectable );
				}
				UniqueKeyMappingMaterializer.materializeUniqueKey(
						ResolvedUniqueKey.explicit(
								table,
								uniqueKeyColumns,
								bindingState.getMetadataBuildingContext(),
								StringHelper.nullIfEmpty( indexAnn.name() ),
								StringHelper.isNotEmpty( indexAnn.name() ),
								indexAnn.options(),
								Arrays.asList( orderings ),
								"table-index"
						)
				);
			}
			else {
				IndexMappingMaterializer.materializeIndex(
						ResolvedIndex.explicit(
								table,
								Arrays.asList( selectables ),
								Arrays.asList( columnExpressions ),
								bindingState.getMetadataBuildingContext(),
								StringHelper.nullIfEmpty( indexAnn.name() ),
								indexAnn.unique(),
								indexAnn.type(),
								indexAnn.using(),
								indexAnn.options(),
								Arrays.asList( orderings ),
								"table-index"
						)
				);
			}
		}
	}

	private List<String> parseColumnList(String columnList) {
		final var tokenizer = new StringTokenizer( columnList, "," );
		final List<String> parsed = new ArrayList<>();
		while ( tokenizer.hasMoreElements() ) {
			final String trimmed = tokenizer.nextToken().trim();
			if ( !trimmed.isEmpty() ) {
				parsed.add( trimmed );
			}
		}
		return parsed;
	}

	private void initializeColumns(String[] columns, String[] orderings, List<String> columnList) {
		for ( int i = 0, size = columnList.size(); i < size; i++ ) {
			final String description = columnList.get( i );
			final String tmp = description.toLowerCase( Locale.ROOT );
			if ( tmp.endsWith( " desc" ) ) {
				columns[i] = description.substring( 0, description.length() - 5 );
				orderings[i] = "desc";
			}
			else if ( tmp.endsWith( " asc" ) ) {
				columns[i] = description.substring( 0, description.length() - 4 );
				orderings[i] = "asc";
			}
			else {
				columns[i] = description;
				orderings[i] = null;
			}
		}
	}

	private Selectable[] selectables(String[] columnNames) {
		final Selectable[] selectables = new Selectable[columnNames.length];
		for ( int i = 0; i < columnNames.length; i++ ) {
			selectables[i] = selectable( columnNames[i] );
		}
		return selectables;
	}

	private Selectable selectable(String columnNameOrFormula) {
		if ( columnNameOrFormula.startsWith( "(" ) ) {
			return new Formula( columnNameOrFormula );
		}
		return createColumn( columnNameOrFormula );
	}

	private Column createColumn(String logicalName) {
		final String physicalName = physicalNamingStrategy
				.toPhysicalColumnName(
						bindingState.getDatabase().toIdentifier( logicalName ),
						jdbcEnvironment
				)
				.render( jdbcEnvironment.getDialect() );
		return new Column( physicalName );
	}

	private void applyRowId(Table table, EntityTypeMetadata type) {
		final RowId rowId = type.getClassDetails().getDirectAnnotationUsage( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
		}
	}
}
