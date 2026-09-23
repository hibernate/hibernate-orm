/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.mapping.NamedTable;

import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.View;
import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.PrimaryTableNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionTableNamingInput;
import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.EntityNamingInput;
import org.hibernate.boot.model.naming.spi.TableNamingInput;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.InlineViewNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
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
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchyImpl;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Join;
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
		final EntityTypeMetadataImpl type = entityBinder.getManagedType();
		final EntityHierarchyImpl.HierarchyRelation hierarchyRelation = entityBinder.getHierarchyRelation();
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

			if ( hierarchyRelation == EntityHierarchyImpl.HierarchyRelation.ROOT ) {
				tableReference = bindPhysicalTable( type, TableSource.from( tableAnn ), true, viewAnn );
			}
			else {
				tableReference = bindUnionTable( entityBinder, TableSource.from( tableAnn ) );
			}
		}
		else if ( type.getHierarchy().getInheritanceType() == InheritanceType.SINGLE_TABLE ) {
			if ( hierarchyRelation == EntityHierarchyImpl.HierarchyRelation.ROOT ) {
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
			EntityTypeMetadataImpl type,
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
		final EntityTypeMetadataImpl type = entityBinder.getManagedType();
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
		if ( !( superTypeTable.binding() instanceof org.hibernate.mapping.PhysicalTable unionBaseTable ) ) {
			throw new MappingException( "Table-per-class entity '" + type.getEntityName()
					+ "' requires a physical superclass table" );
		}

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
				unionBaseTable
		);
		registerLegacyLogicalTableName( logicalName, binding );
		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyUniqueConstraints( binding, tableSource, type.getClassDetails().getName() + " @Table", type.getEntityName() );
		applyIndexes( binding, tableSource, type.getClassDetails().getName() + " @Table",
				PhysicalNamingStrategyHelper.logicalName( logicalName ), type.getEntityName() );

		return new UnionTable( PhysicalNamingStrategyHelper.logicalName( logicalName ), superTypeTable, binding, !type.hasSubTypes() );
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
			EntityTypeMetadataImpl type,
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

		return new InLineView( PhysicalNamingStrategyHelper.logicalName( logicalName ), (org.hibernate.mapping.InlineView) binding );
	}

	private TableReference bindPhysicalTable(
			EntityTypeMetadataImpl type,
			TableSource tableSource,
			boolean isPrimary) {
		return bindPhysicalTable( type, tableSource, isPrimary, null );
	}

	private TableReference bindPhysicalTable(
			EntityTypeMetadataImpl type,
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

	private TableReference bindImplicitPhysicalTable(EntityTypeMetadataImpl type, boolean isPrimary, View viewAnn) {
		final Identifier logicalName = determineLogicalName( type, null );
		final Identifier logicalSchemaName = bindingOptions.getDefaultSchemaName();
		final Identifier logicalCatalogName = bindingOptions.getDefaultCatalogName();

		final Table binding = bindingState.getOrCreateTable(
				toCanonicalName( logicalSchemaName ),
				toCanonicalName( logicalCatalogName ),
				nameForAddTable( logicalName ),
				null,
				type.isAbstract(),
				false,
				viewAnn == null ? null : viewAnn.query()
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, null );

		return createPhysicalTableReference(
				viewAnn,
				PhysicalNamingStrategyHelper.logicalName( logicalName ),
				PhysicalNamingStrategyHelper.logicalName( logicalCatalogName ),
				PhysicalNamingStrategyHelper.logicalName( logicalSchemaName ),
				binding
		);
	}

	private Identifier determineLogicalName(EntityTypeMetadataImpl type, TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment, true );
			}
		}

		return implicitResult( implicitNamingStrategy.determinePrimaryTableName(
				new PrimaryTableNamingInput( entityNaming( type ) ), namingContext() ), "primary table" );
	}

	private TableReference bindExplicitPhysicalTable(
			EntityTypeMetadataImpl type,
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
				tableSource.nonEmptyName() != null,
				viewAnn == null ? null : viewAnn.query()
		);
		registerLegacyLogicalTableName( logicalName, binding );

		applyComment( binding, tableSource );
		applyOptions( binding, tableSource );
		applyType( binding, tableSource );
		applyCheckConstraints( binding, tableSource );
		applyUniqueConstraints( binding, tableSource, type.getClassDetails().getName() + " @Table", type.getEntityName() );
		applyIndexes( binding, tableSource, type.getClassDetails().getName() + " @Table",
				PhysicalNamingStrategyHelper.logicalName( logicalName ), type.getEntityName() );

		return createPhysicalTableReference(
				viewAnn,
				PhysicalNamingStrategyHelper.logicalName( logicalName ),
				PhysicalNamingStrategyHelper.logicalName( logicalCatalogName ),
				PhysicalNamingStrategyHelper.logicalName( logicalSchemaName ),
				binding
		);
	}


	/// Read an already resolved model name without invoking naming or quoting again.
	private static TableReference createPhysicalTableReference(
			View viewAnn,
			LogicalName logicalName,
			LogicalName logicalCatalogName,
			LogicalName logicalSchemaName,
			Table binding) {
		if ( viewAnn != null ) {
			return new PhysicalView(
					logicalName,
					logicalCatalogName,
					logicalSchemaName,
					(org.hibernate.mapping.DatabaseView) binding
			);
		}
		return new PhysicalTable(
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				(org.hibernate.mapping.PhysicalTable) binding
		);
	}

	private void registerLegacyLogicalTableName(Identifier logicalName, Table table) {
		bindingState.getMetadataBuildingContext().getMetadataCollector().addTableNameBinding( logicalName, table );
	}

	public PhysicalTable bindCollectionTable(
			EntityTypeMetadataImpl ownerType,
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
			EntityTypeMetadataImpl ownerType,
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
			EntityTypeMetadataImpl ownerType,
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
			EntityTypeMetadataImpl ownerType,
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
				PhysicalNamingStrategyHelper.logicalName( logicalName ),
				PhysicalNamingStrategyHelper.logicalName( logicalCatalogName ),
				PhysicalNamingStrategyHelper.logicalName( logicalSchemaName ),
				(org.hibernate.mapping.PhysicalTable) binding
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
			EntityTypeMetadataImpl ownerType,
			Table owningTable,
			String attributeName,
			TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment, true );
			}
		}

		return implicitResult( implicitNamingStrategy.determineCollectionTableName(
				new CollectionTableNamingInput( entityNaming( ownerType ), tableNaming( ownerType, owningTable ), attributeName ),
				namingContext() ), "collection table" );
	}

	private Identifier determineAssociationTableLogicalName(
			EntityTypeMetadataImpl ownerType,
			Table owningTable,
			String attributeName,
			EntityNaming targetType,
			Table targetTable,
			TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment, true );
			}
		}

		return implicitResult( implicitNamingStrategy.determineAssociationTableName(
				new AssociationTableNamingInput( entityNaming( ownerType ), tableNaming( ownerType, owningTable ),
						entityNaming( targetType ), tableNaming( targetType, targetTable ), attributeName ),
				namingContext() ), "association table" );
	}

	private ImplicitNamingContext namingContext() {
		return ImplicitNamingContextImpl.forPhysicalNaming( bindingState.getMetadataBuildingContext() );
	}

	private static EntityNamingInput entityNaming(EntityNaming entity) {
		return new EntityNamingInput( entity.getClassName(), entity.getEntityName(), entity.getJpaEntityName() );
	}

	private TableNamingInput tableNaming(EntityNaming entity, Table table) {
		if ( table instanceof org.hibernate.mapping.InlineView view ) {
			return new InlineViewNamingInput( view.getLogicalName() );
		}
		final TableReference ownedReference = entity instanceof org.hibernate.boot.mapping.internal.relational.TableOwner owner
				? bindingState.getTableByOwner( owner ) : null;
		final TableReference reference = ownedReference != null && ownedReference.binding() == table
				? ownedReference : bindingState.getTableByBinding( table );
		if ( reference == null ) {
			throw new MappingException( "No logical table binding for naming dependency: " + table.getName() );
		}
		return new NamedTableNamingInput( new NamingNamePair( reference.logicalName(),
				((org.hibernate.mapping.NamedTable) table).getPhysicalName().objectName() ) );
	}

	private static Identifier implicitResult(LogicalName name, String role) {
		if ( name == null || name.isExplicit() ) {
			throw new MappingException( "Implicit naming strategy must return a non-null implicit name for " + role );
		}
		return PhysicalNamingStrategyHelper.identifier( name );
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
		applyUniqueConstraints( binding, tableSource, entityBinder.getManagedType().getClassDetails().getName()
				+ " @SecondaryTable(name=\"" + secondaryTableAnn.name() + "\")", entityBinder.getManagedType().getEntityName() );
		applyIndexes( binding, tableSource, entityBinder.getManagedType().getClassDetails().getName()
				+ " @SecondaryTable(name=\"" + secondaryTableAnn.name() + "\")",
				PhysicalNamingStrategyHelper.logicalName( logicalName ), entityBinder.getManagedType().getEntityName() );

		final Join join = new Join();
		join.setTable( binding );
		final boolean optional = secondaryRowAnn == null || secondaryRowAnn.optional();
		final boolean owned = secondaryRowAnn == null || secondaryRowAnn.owned();
		join.setOptional( optional );
		join.setInverse( !owned );
		join.setPersistentClass( entityBinder.getTypeBinding() );
		entityBinder.getTypeBinding().addJoin( join );

		return new org.hibernate.boot.mapping.internal.relational.SecondaryTable(
				PhysicalNamingStrategyHelper.logicalName( logicalName ),
				PhysicalNamingStrategyHelper.logicalName( catalogName ),
				PhysicalNamingStrategyHelper.logicalName( schemaName ),
				optional,
				owned,
				primaryKeyJoinColumns( secondaryTableAnn.pkJoinColumns() ),
				ForeignKeySource.firstSpecified(
						ForeignKeySource.from( secondaryTableAnn ),
						ForeignKeySource.fromFirstSpecifiedPrimaryKeyJoinColumn( secondaryTableAnn.pkJoinColumns() )
				),
				(org.hibernate.mapping.PhysicalTable) binding
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
			return BindingHelper.toIdentifier( explicit, target, bindingOptions, jdbcEnvironment, true );
		}

		if ( fallback != null ) {
			return fallback;
		}

		return null;
	}


	private void applyComment(Table table, TableSource tableSource) {
		if ( table instanceof NamedTable namedTable && tableSource != null ) {
			final String comment = tableSource.comment();
			if ( StringHelper.isNotEmpty( comment ) ) {
				namedTable.setComment( comment );
			}
		}
	}

	private void applyOptions(Table table, TableSource tableSource) {
		if ( table instanceof NamedTable namedTable && tableSource != null ) {
			final String options = tableSource.options();
			if ( StringHelper.isNotEmpty( options ) ) {
				namedTable.setOptions( options );
			}
		}
	}

	private void applyType(Table table, TableSource tableSource) {
		if ( table instanceof org.hibernate.mapping.PhysicalTable physicalTable && tableSource != null ) {
			final String type = tableSource.type();
			if ( StringHelper.isNotEmpty( type ) ) {
				physicalTable.setType( type );
			}
		}
	}

	private void applyCheckConstraints(Table table, TableSource tableSource) {
		if ( !(table instanceof org.hibernate.mapping.PhysicalTable physicalTable) || tableSource == null ) {
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
			physicalTable.addCheck( new org.hibernate.mapping.CheckConstraint(
					StringHelper.nullIfEmpty( checkConstraint.name() ),
					checkConstraint.constraint(),
					StringHelper.nullIfEmpty( checkConstraint.options() )
			) );
		}
	}

	private void applyUniqueConstraints(Table table, TableSource tableSource, String location, String entityName) {
		if ( tableSource == null || tableSource.uniqueConstraints() == null ) {
			return;
		}

		for ( int i = 0; i < tableSource.uniqueConstraints().length; i++ ) {
			final var uniqueConstraint = tableSource.uniqueConstraints()[i];
			validateUniqueConstraintColumns( uniqueConstraint.columnNames(), table.getName() );
			UniqueKeyMappingMaterializer.materializeUniqueKey(
					ResolvedUniqueKey.uniqueConstraint( table, Arrays.asList( uniqueConstraint.columnNames() ),
							bindingState.getMetadataBuildingContext(),
							StringHelper.nullIfEmpty( uniqueConstraint.name() ), uniqueConstraint.options(),
							location + ".uniqueConstraints[" + i + "]", entityName, null ) );
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

	private void applyIndexes(Table table, TableSource tableSource, String location, LogicalName logicalTableName, String entityName) {
		if ( !(table instanceof org.hibernate.mapping.PhysicalTable physicalTable) || tableSource == null || tableSource.indexes() == null ) {
			return;
		}
		for ( int i = 0; i < tableSource.indexes().length; i++ ) {
			final var index = tableSource.indexes()[i];
			IndexMappingMaterializer.materializeIndex( new ResolvedIndex(
					physicalTable, logicalTableName, index.columnList(), bindingState.getMetadataBuildingContext(),
					StringHelper.nullIfEmpty( index.name() ), index.unique(), StringHelper.nullIfEmpty( index.type() ),
					StringHelper.nullIfEmpty( index.using() ), StringHelper.nullIfEmpty( index.options() ),
					location + ".indexes[" + i + "]", entityName, null ) );
		}
	}

	private void applyRowId(Table table, EntityTypeMetadataImpl type) {
		final RowId rowId = type.getClassDetails().getDirectAnnotationUsage( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
		}
	}
}
