/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.audit.spi.ChangelogSupplier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.AuxiliaryTableHolder;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Stateful;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.temporal.spi.ChangesetCoordinator;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.annotations.Audited.Table.DEFAULT_CHANGESET_ID_COLUMN_NAME;
import static org.hibernate.annotations.Audited.Table.DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME;
import static org.hibernate.annotations.Audited.Table.DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
import static org.hibernate.audit.AuditStrategy.VALIDITY;
import static org.hibernate.cfg.StateManagementSettings.AUDIT_STRATEGY;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;

/**
 * Helper for building audit log tables in the boot model.
 */
public final class AuditHelper {
	public static final String CHANGESET_ID = "changesetId";
	public static final String MODIFICATION_TYPE = "modificationType";
	public static final String INVALIDATING_CHANGESET_ID = "invalidatingChangesetId";

	private static final String DEFAULT_TABLE_SUFFIX = "_AUD";

	private AuditHelper() {
	}

	static void bindAuditTable(
			Audited.@Nullable Table auditTable,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		bindAuditTable( auditTable, rootClass, context );
		bindSecondaryAuditTables( auditTable, rootClass, classDetails, context );
		bindSubclassAuditTables( auditTable, rootClass, context );
	}

	static void bindAuditTable(
			Audited.@Nullable Table auditTable,
			Collection collection,
			MetadataBuildingContext context) {
		bindAuditTable( auditTable, (Stateful) collection, context );
	}

	private static void bindAuditTable(
			Audited.@Nullable Table auditTable,
			Stateful auditable,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final var table = auditable.getMainTable();
		final String explicitAuditTableName;
		final String auditSchema;
		final String auditCatalog;
		final String csIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			explicitAuditTableName = auditTable.name();
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
			csIdColumnName = auditTable.changesetIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			explicitAuditTableName = "";
			auditSchema = "";
			auditCatalog = "";
			csIdColumnName = DEFAULT_CHANGESET_ID_COLUMN_NAME;
			modTypeColumnName = DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		}
		final boolean hasExplicitAuditTableName = !isBlank( explicitAuditTableName );
		final var auditLogTable = collector.addTable(
				isBlank( auditSchema ) ? table.getSchema() : auditSchema,
				isBlank( auditCatalog ) ? table.getCatalog() : auditCatalog,
				hasExplicitAuditTableName
						? explicitAuditTableName
						: collector.getLogicalTableName( table )
								+ DEFAULT_TABLE_SUFFIX,
				table.getSubselect(),
				table.isAbstract(),
				context,
				hasExplicitAuditTableName
						|| table.getNameIdentifier().isExplicit()
		);
		collector.addTableNameBinding( table.getNameIdentifier(), auditLogTable );

		// Defer audit column creation to a second pass so the transaction
		// ID type is resolved after all entities are bound, including any
		// @Changelog contributed by mapping contributors
		collector.addSecondPass( (OptionalDeterminationSecondPass) ignored -> {
			// Auto-exclude @Version property from audit tables
			if ( auditable instanceof RootClass rootClass && rootClass.isVersioned() ) {
				rootClass.getVersion().setAuditedExcluded( true );
			}
			// Resolve exclusions at second-pass time so collection-managed FK columns
			// (added during collection binding) are detected
			final var excludedColumns = auditable instanceof RootClass rootClass
					? resolveExcludedColumns( rootClass )
					: Set.<String>of();
			copyTableColumns( table, auditLogTable, excludedColumns );
			final var changesetIdColumn =
					createAuditColumn( csIdColumnName,
							getChangesetIdType( context ), auditLogTable, context );
			final var modificationTypeColumn =
					createAuditColumn( modTypeColumnName,
							Byte.class, auditLogTable, context );
			auditLogTable.addColumn( changesetIdColumn );
			auditLogTable.addColumn( modificationTypeColumn );
			if ( auditable instanceof Collection ) {
				// Collection audit PK: (REV, all_source_cols)
				createAuditPrimaryKey( auditLogTable, changesetIdColumn, table.getColumns() );
			}
			else {
				// Entity audit PK: (REV, entity_id_cols) from source table's PK
				createAuditPrimaryKey( auditLogTable, changesetIdColumn, table.getPrimaryKey().getColumns() );
			}
			enableAudit( auditable, auditLogTable, changesetIdColumn, modificationTypeColumn );
			createChangesetForeignKey( auditLogTable, changesetIdColumn, context );
			addTransactionEndColumns( auditTable, auditable, auditLogTable, context );
		} );
	}

	private static void bindSecondaryAuditTables(
			Audited.@Nullable Table auditTable,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final String csIdColumnName;
		final String auditSchema;
		final String auditCatalog;
		if ( auditTable != null ) {
			csIdColumnName = auditTable.changesetIdColumn();
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
		}
		else {
			csIdColumnName = DEFAULT_CHANGESET_ID_COLUMN_NAME;
			auditSchema = null;
			auditCatalog = null;
		}
		final Map<String, String> secondaryAuditTableNames = new HashMap<>();
		classDetails.forEachAnnotationUsage(
				Audited.SecondaryTable.class,
				context.getBootstrapContext().getModelsContext(),
				sat -> secondaryAuditTableNames.put( sat.secondaryTableName(), sat.secondaryAuditTableName() )
		);
		context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored -> {
			for ( var join : rootClass.getJoins() ) {
				final var sourceTable = join.getTable();
				final String customName = secondaryAuditTableNames.get( sourceTable.getName() );
				final var secondaryAuditTable = createAuditTable(
						sourceTable,
						csIdColumnName,
						resolveExcludedColumns( join.getProperties() ),
						nullIfBlank( auditSchema ),
						nullIfBlank( auditCatalog ),
						customName,
						context
				);
				createAuditTableForeignKey( secondaryAuditTable, rootClass.getEntityName(), rootClass.getAuxiliaryTable() );
				// Secondary tables only get tx-id (no mod type, no REVEND)
				join.setAuxiliaryTable( secondaryAuditTable );
				join.addAuxiliaryColumn( CHANGESET_ID, secondaryAuditTable.getPrimaryKey().getColumn( 0 ) );
			}
		} );
	}

	private static void bindSubclassAuditTables(
			Audited.@Nullable Table auditTable,
			RootClass rootClass,
			MetadataBuildingContext context) {
		final String csIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			csIdColumnName = auditTable.changesetIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			csIdColumnName = DEFAULT_CHANGESET_ID_COLUMN_NAME;
			modTypeColumnName = DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		}
		// Defer to second pass since subclasses haven't been added to rootClass yet
		context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored ->
				bindSubclassAuditTables(
						rootClass,
						auditTable,
						csIdColumnName,
						modTypeColumnName,
						context
				)
		);
	}

	/**
	 * Create audit tables for direct subclasses of {@code parent},
	 * then recurse into their children.
	 */
	private static void bindSubclassAuditTables(
			PersistentClass parent,
			Audited.@Nullable Table auditTable,
			String csIdColumnName,
			String modTypeColumnName,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		for ( var subclass : parent.getDirectSubclasses() ) {
			if ( subclass instanceof TableOwner ) {
				// Check if the subclass has its own @Audited.Table for table name/schema/catalog override
				final var subclassDetails = modelsContext.getClassDetailsRegistry()
						.getClassDetails( subclass.getClassName() );
				final var subclassTable = subclassDetails.getDirectAnnotationUsage( Audited.Table.class );
				final var effective = subclassTable != null ? subclassTable : auditTable;
				final var subclassAuditTable = createAuditTable(
						subclass.getTable(),
						csIdColumnName,
						resolveExcludedColumns( subclass.getProperties() ),
						effective != null ? nullIfBlank( effective.schema() ) : null,
						effective != null ? nullIfBlank( effective.catalog() ) : null,
						effective != null ? nullIfBlank( effective.name() ) : null,
						context
				);
				subclass.addAuxiliaryColumn( CHANGESET_ID, subclassAuditTable.getPrimaryKey().getColumn( 0 ) );
				if ( subclass instanceof UnionSubclass ) {
					// TABLE_PER_CLASS: each table is self-contained, needs its own REVTYPE and REVEND
					final var modificationTypeColumn =
							createAuditColumn( modTypeColumnName,
									Byte.class, subclassAuditTable, context );
					subclassAuditTable.addColumn( modificationTypeColumn );
					subclass.addAuxiliaryColumn( MODIFICATION_TYPE, modificationTypeColumn );
					addTransactionEndColumns( auditTable, subclass, subclassAuditTable, context );
				}
				else {
					// JOINED: REVTYPE/REVEND only on root table; FK to parent audit table
					createAuditTableForeignKey(
							subclassAuditTable,
							parent.getEntityName(),
							parent.getAuxiliaryTable()
					);
				}
				subclass.setAuxiliaryTable( subclassAuditTable );
				// Recurse into this subclass's children
				bindSubclassAuditTables( subclass, auditTable, csIdColumnName, modTypeColumnName, context );
			}
		}
	}

	static void enableAudit(
			Stateful model, Table auditTable,
			Column changesetIdColumn, Column modificationTypeColumn) {
		model.setAuxiliaryTable( auditTable );
		model.addAuxiliaryColumn( CHANGESET_ID, changesetIdColumn );
		model.addAuxiliaryColumn( MODIFICATION_TYPE, modificationTypeColumn );
		model.setStateManagementType( AuditStateManagement.class );
	}

	/**
	 * Create a middle audit table for unidirectional @OneToMany @JoinColumn.
	 * The table tracks collection membership with (parent_key, child_key, REV, REVTYPE)
	 * <p>
	 * The child entity's FK column is on the child table, but from an entity model
	 * perspective the collection is part of the parent entity's state.
	 */
	static void bindOneToManyAuditTable(
			Audited.@Nullable Table auditTable,
			Collection collection,
			String referencedEntityName,
			Audited.@Nullable CollectionTable collectionAuditTable,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final var ownerTable = collection.getOwner().getTable();

		// Table name: @Audited.CollectionTable name, or {OwnerJpaEntityName}_{ChildJpaEntityName}_AUD
		final var referencedEntity = collector.getEntityBinding( referencedEntityName );
		final String auditTableName;
		if ( collectionAuditTable != null && !isBlank( collectionAuditTable.name() ) ) {
			auditTableName = collectionAuditTable.name();
		}
		else {
			final String ownerSimpleName = collection.getOwner().getJpaEntityName();
			final String childSimpleName = referencedEntity.getJpaEntityName();
			auditTableName = ownerSimpleName + "_" + childSimpleName + DEFAULT_TABLE_SUFFIX;
		}

		final String auditSchema;
		final String auditCatalog;
		final String csIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
			csIdColumnName = auditTable.changesetIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			auditSchema = "";
			auditCatalog = "";
			csIdColumnName = DEFAULT_CHANGESET_ID_COLUMN_NAME;
			modTypeColumnName = DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		}
		final String schema = collectionAuditTable != null && !isBlank( collectionAuditTable.schema() )
				? collectionAuditTable.schema()
				: !isBlank( auditSchema ) ? auditSchema : ownerTable.getSchema();
		final String catalog = collectionAuditTable != null && !isBlank( collectionAuditTable.catalog() )
				? collectionAuditTable.catalog()
				: !isBlank( auditCatalog ) ? auditCatalog : ownerTable.getCatalog();
		final var middleAuditTable = collector.addTable(
				schema,
				catalog,
				auditTableName,
				null,
				false,
				context,
				false
		);
		collector.addSecondPass( (OptionalDeterminationSecondPass) ignored -> {
			final var keyColumns = new ArrayList<Column>();
			// Copy the FK columns (parent key) from the collection's key
			for ( var column : collection.getKey().getColumns() ) {
				final var copy = column.clone();
				copy.setUnique( false );
				copy.setUniqueKeyName( null );
				middleAuditTable.addColumn( copy );
				keyColumns.add( copy );
			}
			// Copy the child identifier columns from the referenced entity
			for ( var column : referencedEntity.getKey().getColumns() ) {
				final var copy = column.clone();
				copy.setUnique( false );
				copy.setUniqueKeyName( null );
				middleAuditTable.addColumn( copy );
				keyColumns.add( copy );
			}
			// Audit columns
			final var changesetIdColumn = createAuditColumn(
					csIdColumnName,
					getChangesetIdType( context ),
					middleAuditTable,
					context
			);
			final var modificationTypeColumn = createAuditColumn(
					modTypeColumnName,
					Byte.class,
					middleAuditTable,
					context
			);
			middleAuditTable.addColumn( changesetIdColumn );
			middleAuditTable.addColumn( modificationTypeColumn );
			createAuditPrimaryKey( middleAuditTable, changesetIdColumn, keyColumns );
			createChangesetForeignKey( middleAuditTable, changesetIdColumn, context );
			enableAudit( collection, middleAuditTable, changesetIdColumn, modificationTypeColumn );
			addTransactionEndColumns( auditTable, collection, middleAuditTable, context );
		} );
	}

	static void bindChangelog(
			Changelog changelog,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();

		// note : @Changelog currently requires @Entity as well

		// The entity must not be audited
		if ( classDetails.hasAnnotationUsage( Audited.class, modelsContext ) ) {
			throw new MappingException( "The @Changelog entity cannot be audited" );
		}

		// Scan class members (including supertypes) for @ChangesetId,
		// @Timestamp, and @ModifiedEntities. We need the names
		// and type eagerly to configure the supplier before audit table
		// second passes create the REV column.
		MemberDetails revNumberMember = null;
		MemberDetails revTimestampMember = null;
		MemberDetails modifiedEntityNamesMember = null;
		for ( var current = classDetails; current != null; current = current.getSuperClass() ) {
			for ( var member : current.getFields() ) {
				revNumberMember = checkAnnotation(
						member,
						revNumberMember,
						Changelog.ChangesetId.class,
						classDetails
				);
				revTimestampMember = checkAnnotation(
						member,
						revTimestampMember,
						Changelog.Timestamp.class,
						classDetails
				);
				modifiedEntityNamesMember = checkAnnotation(
						member,
						modifiedEntityNamesMember,
						Changelog.ModifiedEntities.class,
						classDetails
				);
			}
			for ( var member : current.getMethods() ) {
				revNumberMember = checkAnnotation(
						member,
						revNumberMember,
						Changelog.ChangesetId.class,
						classDetails
				);
				revTimestampMember = checkAnnotation(
						member,
						revTimestampMember,
						Changelog.Timestamp.class,
						classDetails
				);
				modifiedEntityNamesMember = checkAnnotation(
						member,
						modifiedEntityNamesMember,
						Changelog.ModifiedEntities.class,
						classDetails
				);
			}
		}

		if ( revNumberMember == null ) {
			throw new MappingException(
					"@Changelog '" + classDetails.getName()
							+ "' must have a property annotated with @Changelog.ChangesetId"
			);
		}
		if ( revTimestampMember == null ) {
			throw new MappingException(
					"@Changelog '" + classDetails.getName()
							+ "' must have a property annotated with @Changelog.Timestamp"
			);
		}

		// Configure the supplier eagerly
		final var serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final var listenerClass = changelog.listener();
		final var listener = listenerClass != ChangesetListener.class
				? serviceRegistry.requireService( ManagedBeanRegistry.class )
						.getBean( listenerClass ).getBeanInstance()
				: null;
		final var supplier = new ChangelogSupplier<>(
				classDetails.toJavaClass(),
				revNumberMember.resolveAttributeName(),
				revTimestampMember.resolveAttributeName(),
				modifiedEntityNamesMember != null
						? modifiedEntityNamesMember.resolveAttributeName()
						: null, listener
		);
		final var revNumberType = revNumberMember.getType().determineRawClass().toJavaClass();
		serviceRegistry.requireService( ChangesetCoordinator.class )
				.contributeIdentifierSupplier( supplier, revNumberType );

		// Defer validation (basic type, mapped as Hibernate property) and
		// unique constraint to second pass when entity properties are fully bound
		final String entityName = rootClass.getEntityName();
		final String revNumberName = revNumberMember.resolveAttributeName();
		final String revTimestampName = revTimestampMember.resolveAttributeName();
		context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored ->
				validateChangelog( entityName, revNumberName, revTimestampName, context )
		);
	}

	/**
	 * Check if a member has the given annotation. If found, validate no
	 * duplicate and return the member; otherwise return the existing value.
	 */
	private static MemberDetails checkAnnotation(
			MemberDetails member,
			@Nullable MemberDetails existing,
			Class<? extends Annotation> annotationType,
			ClassDetails classDetails) {
		if ( member.hasDirectAnnotationUsage( annotationType ) ) {
			if ( existing != null ) {
				throw new MappingException(
						"@Changelog '" + classDetails.getName()
								+ "' has multiple members annotated with @"
								+ annotationType.getSimpleName()
				);
			}
			return member;
		}
		return existing;
	}

	/**
	 * Second-pass validation: verify {@code @Changelog.ChangesetId}
	 * and {@code @Changelog.Timestamp} are mapped as basic properties,
	 * and add a unique constraint on non-ID {@code @ChangesetId}.
	 */
	private static void validateChangelog(
			String entityName,
			String revNumberName,
			String revTimestampName,
			MetadataBuildingContext context) {
		final var entityBinding = context.getMetadataCollector().getEntityBinding( entityName );
		if ( entityBinding == null ) {
			return;
		}
		final var revNumberProperty = requireBasicProperty(
				entityBinding,
				revNumberName,
				"@Changelog.ChangesetId"
		);
		requireBasicProperty( entityBinding, revTimestampName, "@Changelog.Timestamp" );
		// Add unique constraint on non-ID @ChangesetId
		if ( revNumberProperty != entityBinding.getIdentifierProperty() ) {
			for ( var column : revNumberProperty.getColumns() ) {
				column.setUnique( true );
			}
		}
	}

	/**
	 * Validate that a named property exists and is mapped as a {@link BasicValue}.
	 */
	private static Property requireBasicProperty(
			PersistentClass entityBinding,
			String propertyName,
			String annotationName) {
		final Property property;
		try {
			property = entityBinding.getProperty( propertyName );
		}
		catch (MappingException e) {
			throw new MappingException(
					annotationName + " member '" + propertyName
							+ "' is not mapped as a property on @Changelog '"
							+ entityBinding.getEntityName() + "'"
			);
		}
		if ( !( property.getValue() instanceof BasicValue ) ) {
			throw new MappingException(
					annotationName + " property '" + entityBinding.getEntityName()
							+ "." + propertyName + "' must be a basic attribute"
			);
		}
		return property;
	}

	/**
	 * Create an audit table for the given source table: copy columns,
	 * add the REV column, create the composite PK, and add the
	 * REV -> REVINFO FK (if a changelog entity is configured).
	 */
	private static Table createAuditTable(
			Table sourceTable,
			String csIdColumnName,
			Set<String> excludedColumns,
			@Nullable String schemaOverride,
			@Nullable String catalogOverride,
			@Nullable String customAuditTableName,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final String auditTableName = customAuditTableName != null
				? customAuditTableName
				: collector.getLogicalTableName( sourceTable ) + DEFAULT_TABLE_SUFFIX;
		final var auditTable = collector.addTable(
				schemaOverride != null ? schemaOverride : sourceTable.getSchema(),
				catalogOverride != null ? catalogOverride : sourceTable.getCatalog(),
				auditTableName,
				sourceTable.getSubselect(),
				sourceTable.isAbstract(),
				context,
				sourceTable.getNameIdentifier().isExplicit()
		);
		copyTableColumns( sourceTable, auditTable, excludedColumns );
		final var revColumn = createAuditColumn( csIdColumnName, getChangesetIdType( context ), auditTable, context );
		auditTable.addColumn( revColumn );
		createAuditPrimaryKey( auditTable, revColumn, sourceTable.getPrimaryKey().getColumns() );
		createChangesetForeignKey( auditTable, revColumn, context );
		return auditTable;
	}

	private static void createAuditPrimaryKey(
			Table auditTable,
			Column changesetIdColumn,
			Iterable<Column> sourceKeyColumns) {
		final var pk = new PrimaryKey( auditTable );
		pk.addColumn( changesetIdColumn );
		for ( var sourceCol : sourceKeyColumns ) {
			pk.addColumn( auditTable.getColumn( sourceCol ) );
		}
		auditTable.setPrimaryKey( pk );
	}

	private static Class<?> getChangesetIdType(MetadataBuildingContext context) {
		return context.getBootstrapContext().getServiceRegistry()
				.requireService( ChangesetCoordinator.class )
				.getIdentifierType();
	}

	private static void copyTableColumns(Table sourceTable, Table targetTable, Set<String> excludedColumns) {
		for ( var column : sourceTable.getColumns() ) {
			if ( !excludedColumns.contains( column.getCanonicalName() ) ) {
				final var copy = column.clone();
				// Audit tables must not inherit unique constraints from the source,
				// since the same value can appear at different revisions
				copy.setUnique( false );
				copy.setUniqueKeyName( null );
				targetTable.addColumn( copy );
			}
		}
	}

	private static Column createAuditColumn(
			String columnName,
			Class<?> javaType,
			Table table,
			MetadataBuildingContext context) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> javaType );
		final var column = new Column();
		column.setNullable( false );
		column.setValue( basicValue );
		basicValue.addColumn( column );

		final var database = context.getMetadataCollector().getDatabase();
		setColumnName( columnName, column, database, context.getBuildingOptions().getPhysicalNamingStrategy() );
		setTemporalColumnType( column, database, javaType );

		return column;
	}

	private static void setTemporalColumnType(
			Column column,
			Database database,
			Class<?> javaType) {
		if ( Instant.class.equals( javaType ) ) {
			final var temporalTableSupport = database.getDialect().getTemporalTableSupport();
			column.setTemporalPrecision( temporalTableSupport.getTemporalColumnPrecision() );
			column.setSqlTypeCode( temporalTableSupport.getTemporalColumnType() );
		}
	}

	private static void setColumnName(
			String name,
			Column column,
			Database database,
			PhysicalNamingStrategy physicalNamingStrategy) {
		final Identifier physicalColumnName =
				physicalNamingStrategy.toPhysicalColumnName(
						database.toIdentifier( name ),
						database.getJdbcEnvironment()
				);
		column.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private static boolean isValidityStrategy(MetadataBuildingContext context) {
		return context.getAuditStrategy() == VALIDITY;
	}

	private static void addTransactionEndColumns(
			Audited.@Nullable Table auditTableAnnotation,
			AuxiliaryTableHolder holder,
			Table auditTable,
			MetadataBuildingContext context) {
		if ( !isValidityStrategy( context ) ) {
			return;
		}
		final var revEndColumn =
				createAuditColumn(
						auditTableAnnotation != null
								? auditTableAnnotation.invalidatingChangesetIdColumn()
								: DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME,
						getChangesetIdType( context ), auditTable, context );
		revEndColumn.setNullable( true );
		auditTable.addColumn( revEndColumn );
		holder.addAuxiliaryColumn( INVALIDATING_CHANGESET_ID, revEndColumn );
		createChangesetForeignKey( auditTable, revEndColumn, context );
	}

	/**
	 * Create a FK from the audit table's REV (or REVEND) column to the
	 * changelog entity's PK. Only applies when {@code @Changelog}
	 * is configured.
	 */
	private static void createChangesetForeignKey(
			Table auditTable,
			Column revColumn,
			MetadataBuildingContext context) {
		final String changelogName = getChangelogName( context );
		if ( changelogName != null ) {
			auditTable.createForeignKey(
					null,
					List.of( revColumn ),
					changelogName,
					null,
					null
			);
		}
	}

	/**
	 * Create a FK from one audit table's PK to another audit table's PK.
	 * Used for JOINED inheritance (child_aud -> parent_aud) and
	 * {@code @SecondaryTable} (secondary_aud -> primary_aud).
	 */
	private static void createAuditTableForeignKey(
			Table sourceAuditTable,
			String rootEntityName,
			Table referencedAuditTable) {
		final var fk = sourceAuditTable.createForeignKey(
				null,
				new ArrayList<>( sourceAuditTable.getPrimaryKey().getColumns() ),
				rootEntityName,
				null,
				null
		);
		fk.setReferencedTable( referencedAuditTable );
	}

	private static @Nullable String getChangelogName(MetadataBuildingContext context) {
		final var supplier = ChangelogSupplier.resolve( context.getBootstrapContext().getServiceRegistry() );
		return supplier != null ? supplier.getChangelogClass().getName() : null;
	}

	private static Set<String> resolveExcludedColumns(Iterable<Property> properties) {
		final Set<String> excluded = new HashSet<>();
		for ( var property : properties ) {
			if ( property.isAuditedExcluded() || property instanceof Backref ) {
				for ( var column : property.getColumns() ) {
					excluded.add( column.getCanonicalName() );
				}
			}
		}
		return excluded;
	}

	private static Set<String> resolveExcludedColumns(RootClass rootClass) {
		final Set<String> excluded = new HashSet<>();
		final Set<String> mappedColumns = new HashSet<>();
		// Identifier columns
		for ( var column : rootClass.getIdentifier().getColumns() ) {
			mappedColumns.add( column.getCanonicalName() );
		}
		// Discriminator column
		if ( rootClass.getDiscriminator() != null ) {
			for ( var column : rootClass.getDiscriminator().getColumns() ) {
				mappedColumns.add( column.getCanonicalName() );
			}
		}
		// All properties in the hierarchy (root + subclasses for SINGLE_TABLE)
		collectPropertyColumns( rootClass, mappedColumns, excluded );
		for ( var subclass : rootClass.getSubclasses() ) {
			collectPropertyColumns( subclass, mappedColumns, excluded );
		}
		// Exclude unmapped columns (e.g. FK from unidirectional @OneToMany @JoinColumn)
		for ( var column : rootClass.getMainTable().getColumns() ) {
			if ( !mappedColumns.contains( column.getCanonicalName() ) ) {
				excluded.add( column.getCanonicalName() );
			}
		}
		return excluded;
	}

	private static void collectPropertyColumns(
			PersistentClass persistentClass,
			Set<String> mappedColumns,
			Set<String> excluded) {
		for ( var property : persistentClass.getProperties() ) {
			if ( property.isAuditedExcluded() || property instanceof Backref ) {
				for ( var column : property.getColumns() ) {
					excluded.add( column.getCanonicalName() );
				}
			}
			else {
				for ( var column : property.getColumns() ) {
					mappedColumns.add( column.getCanonicalName() );
				}
			}
		}
	}

	// --- Runtime helpers ---

	/**
	 * Whether the given fetchable is excluded from auditing and the
	 * current context is loading from an audit table. Returns
	 * {@code false} immediately when there is no temporal identifier
	 * (the common case for non-audit queries).
	 */
	public static boolean isFetchableAuditExcluded(Fetchable fetchable, LoadQueryInfluencers influencers) {
		if ( influencers.getTemporalIdentifier() == null ) {
			return false;
		}
		final var attr = fetchable.asAttributeMapping();
		if ( attr != null
				&& attr.getStateArrayPosition() >= 0
				&& attr.getDeclaringType() instanceof EntityMappingType entityMappingType ) {
			final var persister = entityMappingType.getEntityPersister();
			return persister.getAuditMapping() != null
					&& persister.isPropertyAuditedExcluded( attr.getStateArrayPosition() );
		}
		return false;
	}

	public static AuditStrategy determineAuditStrategy(Map<String, Object> configurationSettings) {
		final Object setting = configurationSettings.get( AUDIT_STRATEGY );
		if ( setting instanceof AuditStrategy auditStrategy ) {
			return auditStrategy;
		}
		else if ( setting instanceof String string ) {
			for ( var strategy : AuditStrategy.values() ) {
				if ( strategy.name().equalsIgnoreCase( string ) ) {
					return strategy;
				}
			}
		}
		return AuditStrategy.DEFAULT;
	}
}
