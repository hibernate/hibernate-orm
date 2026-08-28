/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
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
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.temporal.spi.ChangesetCoordinator;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			@Nullable Audited.Table auditTable,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		bindAuditTable( auditTable , rootClass, context, null);
		bindSecondaryAuditTables( auditTable, rootClass, classDetails, context );
		bindSubclassAuditTables( auditTable, rootClass, context );
	}

	static void bindAuditTable(
			@Nullable Audited.Table auditTable,
			Collection collection,
			MetadataBuildingContext context,
			@Nullable Audited.CollectionTable collectionTableOverride) {
		bindAuditTable(
				auditTable,
				(Stateful) collection,
				context,
				collectionTableOverride
		);
	}

	private static void bindAuditTable(
			@Nullable Audited.Table auditTable,
			Stateful auditable,
			MetadataBuildingContext context,
			@Nullable Audited.CollectionTable collectionTableOverride
	) {
		final var collector = context.getMetadataCollector();
		final var table = auditable.getMainTable();
		final String explicitAuditTableName;
		final String auditSchema;
		final String auditCatalog;
		final String csIdColumnName;
		final String modTypeColumnName;
		if ( collectionTableOverride != null ) {
			explicitAuditTableName = collectionTableOverride.name();
			auditSchema = collectionTableOverride.schema();
			auditCatalog = collectionTableOverride.catalog();
			csIdColumnName = Audited.Table.DEFAULT_CHANGESET_ID_COLUMN_NAME;
			modTypeColumnName = Audited.Table.DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		} else if ( auditTable != null ) {
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
					? resolveExcludedColumns( rootClass, context )
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
			@Nullable Audited.Table auditTable,
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
						resolveExcludedColumns( join.getProperties(), null, context.getBootstrapContext().getModelsContext() ), //TODO
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
			@Nullable Audited.Table auditTable,
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
			@Nullable Audited.Table auditTable,
			String csIdColumnName,
			String modTypeColumnName,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		for ( var subclass : parent.getDirectSubclasses() ) {
			if ( subclass instanceof TableOwner ) { //TODO ich glaube für JOINED und TABLE_PER_CLASS ist das hier true, nicht?
				// Check if the subclass has its own @Audited.Table for table name/schema/catalog override
				final var subclassDetails = modelsContext.getClassDetailsRegistry()
						.getClassDetails( subclass.getClassName() );
				final var subclassTable = subclassDetails.getDirectAnnotationUsage( Audited.Table.class );
				final var effective = subclassTable != null ? subclassTable : auditTable;
				final var subclassAuditTable = createAuditTable(
						subclass.getTable(),
						csIdColumnName,
						resolveExcludedColumns( subclass.getProperties(), subclass, context.getBootstrapContext().getModelsContext() ),
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
			@Nullable Audited.Table auditTable,
			Collection collection,
			String referencedEntityName,
			@Nullable Audited.CollectionTable collectionAuditTable,
			MetadataBuildingContext context,
			@Nullable Audited.CollectionTable collectionAuditTableOverride) {
		final var collector = context.getMetadataCollector();
		final var ownerTable = collection.getOwner().getTable();

		// Table name: @Audited.CollectionTable name (if applicable, taken from @AuditOverride), or {OwnerJpaEntityName}_{ChildJpaEntityName}_AUD
		final var referencedEntity = collector.getEntityBinding( referencedEntityName );
		final String auditTableName =
				auditTableName( collection, collectionAuditTable, referencedEntity, collectionAuditTableOverride );

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
		final String schema =
				collectionAuditTableOverride != null && !isBlank( collectionAuditTableOverride.schema() )
						? collectionAuditTableOverride.schema() :
						collectionAuditTable != null && !isBlank( collectionAuditTable.schema() )
								? collectionAuditTable.schema()
								: !isBlank( auditSchema ) ? auditSchema : ownerTable.getSchema();
		final String catalog =
				collectionAuditTableOverride != null && !isBlank( collectionAuditTableOverride.catalog() )
						? collectionAuditTableOverride.catalog() :
				collectionAuditTable != null && !isBlank( collectionAuditTable.catalog() )
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
				keyColumns.add( copyColumnRemovingUnique( column, middleAuditTable ) );
			}
			// Copy the child identifier columns from the referenced entity
			for ( var column : referencedEntity.getKey().getColumns() ) {
				keyColumns.add( copyColumnRemovingUnique( column, middleAuditTable ) );
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

	private static void addOverridesToMap(PersistentClass pc, HashMap<String, Audited.Override> overridesMap, ModelsContext modelsContext) {
		var classToScan = pc.getClassName();
		collectOverrides( classToScan, overridesMap, modelsContext );
		var msc = pc.getSuperMappedSuperclass();
		while ( msc != null ) {
			collectOverrides( msc.getMappedClass().getName(), overridesMap, modelsContext );
			msc = msc.getSuperMappedSuperclass();
		}
	}

	@Nonnull
	private static String auditTableName(
			Collection collection,
			@Nullable Audited.CollectionTable collectionAuditTable,
			PersistentClass referencedEntity,
			@Nullable Audited.CollectionTable collectionAuditTableOverride) {

		if ( collectionAuditTableOverride != null && !collectionAuditTableOverride.name().isBlank()) {
			return collectionAuditTableOverride.name();
		}

		// search name in Audited.CollectionTable
		if ( collectionAuditTable != null && !isBlank( collectionAuditTable.name() ) ) {
			return collectionAuditTable.name();
		}
		else {
			final String ownerSimpleName = collection.getOwner().getJpaEntityName();
			final String childSimpleName = referencedEntity.getJpaEntityName();
			return ownerSimpleName + "_" + childSimpleName + DEFAULT_TABLE_SUFFIX;
		}
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
		if ( entityBinding != null ) {
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
				copyColumnRemovingUnique( column, targetTable );
			}
		}
	}

	private static Column copyColumnRemovingUnique(Column sourceColumn, Table auditTable) {
		final var auditColumn = copyColumn( auditTable, sourceColumn );
		removeUniqueConstraint( auditColumn );
		return auditColumn;
	}

	@Nonnull
	private static Column copyColumn(Table targetTable, Column column) {
		final var targetColumn = targetTable.getColumn( column );
		if ( targetColumn == null ) {
			final var columnCopy = column.clone();
			columnCopy.copy( column );
			targetTable.addColumn( columnCopy );
			return columnCopy;
		}
		else {
			targetColumn.copy( column );
			return targetColumn;
		}
	}

	private static void removeUniqueConstraint(Column column) {
		// Audit tables must not inherit unique constraints from the source,
		// since the same value can appear at different revisions
		column.setUnique( false );
		column.setUniqueKeyName( null );
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
		setColumnName( columnName, column, database,
				context.getBuildingOptions().getPhysicalNamingStrategy() );
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
			@Nullable Audited.Table auditTableAnnotation,
			AuxiliaryTableHolder holder,
			Table auditTable,
			MetadataBuildingContext context) {
		if ( isValidityStrategy( context ) ) {
			final var revEndColumn =
					createAuditColumn(
							auditTableAnnotation != null ?
									auditTableAnnotation.invalidatingChangesetIdColumn()
									: DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME,
							getChangesetIdType( context ), auditTable, context );
			revEndColumn.setNullable( true );
			auditTable.addColumn( revEndColumn );
			holder.addAuxiliaryColumn( INVALIDATING_CHANGESET_ID, revEndColumn );
			createChangesetForeignKey( auditTable, revEndColumn, context );
		}
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

	private static Set<String> resolveExcludedColumns(Iterable<Property> properties, PersistentClass pc, ModelsContext mc) {
		final Set<String> excluded = new HashSet<>();
		for ( var property : properties ) {
			if ( property.isAuditedExcluded() || property instanceof Backref ) {
				for ( var column : property.getColumns() ) {
					excluded.add( column.getCanonicalName() );
				}
			}
		}
		if ( pc != null ) {
			var overridesMap = new HashMap<String, Audited.Override>();
			addOverridesToMap( pc, overridesMap, mc );
			overridesMap.forEach( (str, annotation ) -> {
				if ( !annotation.isAudited() ) {
					excluded.add( str ); //TODO column names instead of property names
				}
			} );
		}

		return excluded;
	}

	private static Set<String> resolveExcludedColumns(RootClass rootClass, MetadataBuildingContext context) {
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
		var modelsContext = context.getBootstrapContext().getModelsContext();

		collectPropertyColumns( rootClass, mappedColumns, excluded, modelsContext );
		for ( var subclass : rootClass.getSubclasses() ) {
			collectPropertyColumns( subclass, mappedColumns, excluded, modelsContext );
		}
		// Exclude unmapped columns (e.g. FK from unidirectional @OneToMany @JoinColumn)
		for ( var column : rootClass.getMainTable().getColumns() ) {
			if ( !mappedColumns.contains( column.getCanonicalName() ) ) {
				excluded.add( column.getCanonicalName() );
			}
		}
		return excluded;
	}

	private static InheritanceType getInheritanceStrategy(String className, ModelsContext context) {
		var classDetails = context.getClassDetailsRegistry()
				.getClassDetails( className )
				.getAnnotationUsage( Inheritance.class, context );
		return classDetails == null ?  InheritanceType.SINGLE_TABLE : classDetails.strategy();
	}

	static Audited.CollectionTable extractLowestCollectionTableAuditOverrideFromHierarchy(PersistentClass persistentClass, ModelsContext modelsContext, String propertyName) {
		var fullHierarchy = new ArrayList<PersistentClass>( persistentClass.getSubclasses() );
		fullHierarchy.add( persistentClass );
		for ( var pc : fullHierarchy ) {
			var auditOverride = findAuditOverride( propertyName,
					modelsContext.getClassDetailsRegistry().getClassDetails( pc.getClassName() ), modelsContext );
			if ( auditOverride != null ) {
				return auditOverride.collectionTable();
			}
		}
		return null;
	}

	private static void collectOverrides(String classToScan, HashMap<String, Audited.Override> overrides, ModelsContext modelsContext) {
		var registry = modelsContext.getClassDetailsRegistry();
		if (classToScan == null) return;
		registry.getClassDetails( classToScan )
				.forEachAnnotationUsage( Audited.Override.class, modelsContext,
						override -> overrides.putIfAbsent( override.name(), override )
				);
	}

	private static void collectPropertyColumns(
			PersistentClass persistentClass,
			Set<String> mappedColumns,
			Set<String> excluded,
			ModelsContext modelsContext) {
		for ( var property : persistentClass.getProperties() ) {
			if ( isEffectivelyExcluded(
					modelsContext,
					persistentClass,
					property.getName(),
					property.isAuditedExcluded()
			) || property instanceof Backref ) {
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


	static boolean isEffectivelyExcluded(ModelsContext modelsContext, PersistentClass persistentClass, String propertyName, boolean excludedAtDeclaration) {
		var classDetails = modelsContext.getClassDetailsRegistry().getClassDetails( persistentClass.getClassName() );
		var override = findAuditOverride( propertyName, classDetails, modelsContext );
		var inheritanceStrategy = getInheritanceStrategy( persistentClass.getClassName(), modelsContext );

		/*
		 * A property is initially excluded in two cases:
		 * 1) 	At declaration, it has an @Audited.Excluded annotation
		 * 2) 	If the property is inherited from a @MappedSuperClass and there is
		 * 	  	an @Audited.Override(name="prop", isAudited = false) annotation on the @Entity class or a @MappedSuperClass
		 * 	    in between.
		 */
		boolean initiallyExcluded = excludedAtDeclaration;
		if ( override != null ) {
			initiallyExcluded = !override.isAudited();
		}
		return initiallyExcluded && !isRevoked( propertyName, persistentClass, inheritanceStrategy == InheritanceType.SINGLE_TABLE, modelsContext );
	}

	static @Nullable Audited.Override findAuditOverride(
			String propertyName,
			ClassDetails classDetails,
			ModelsContext modelsContext) {
		var current = classDetails;
		while ( current != null ) {
			var override = current.getNamedAnnotationUsage(
					Audited.Override.class, propertyName, "name", modelsContext
			);
			if ( override != null ) {
				return override;
			}
			current = current.getSuperClass();
		}
		return null;
	}

	static boolean isRevoked(String propertyName, PersistentClass persistentClass, boolean processSubclasses, ModelsContext modelsContext) {
		var classesToScan = new ArrayList<PersistentClass>();
		if ( processSubclasses ) {
			classesToScan.addAll( persistentClass.getSubclasses() );
		}
		classesToScan.add( persistentClass );
		for ( var pc : classesToScan ) {
			var auditOverride = findAuditOverride( propertyName,
					modelsContext.getClassDetailsRegistry().getClassDetails( pc.getClassName() ), modelsContext );
			if ( auditOverride != null && auditOverride.isAudited() ) {
				return true;
			}
		}
		return false;
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
