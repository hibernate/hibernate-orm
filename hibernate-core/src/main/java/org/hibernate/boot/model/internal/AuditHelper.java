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
import org.hibernate.annotations.RevisionEntity;
import org.hibernate.audit.RevisionListener;
import org.hibernate.audit.spi.RevisionEntitySupplier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
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
import org.hibernate.temporal.spi.TransactionIdentifierService;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;

/**
 * Helper for building audit log tables in the boot model.
 */
public final class AuditHelper {
	public static final String TRANSACTION_ID = "transactionId";
	public static final String MODIFICATION_TYPE = "modificationType";
	public static final String TRANSACTION_END = "transactionEnd";
	public static final String TRANSACTION_END_TIMESTAMP = "transactionEndTimestamp";

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
		final String txIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			explicitAuditTableName = auditTable.name();
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
			txIdColumnName = auditTable.transactionIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			explicitAuditTableName = "";
			auditSchema = "";
			auditCatalog = "";
			txIdColumnName = Audited.Table.DEFAULT_TRANSACTION_ID;
			modTypeColumnName = Audited.Table.DEFAULT_MODIFICATION_TYPE;
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
		// @RevisionEntity contributed by mapping contributors
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
			final var transactionIdColumn =
					createAuditColumn( txIdColumnName,
							getTransactionIdType( context ), auditLogTable, context );
			final var modificationTypeColumn =
					createAuditColumn( modTypeColumnName,
							Byte.class, auditLogTable, context );
			auditLogTable.addColumn( transactionIdColumn );
			auditLogTable.addColumn( modificationTypeColumn );
			if ( auditable instanceof Collection ) {
				// Collection audit PK: (REV, all_source_cols)
				createAuditPrimaryKey( auditLogTable, transactionIdColumn, table.getColumns() );
			}
			else {
				// Entity audit PK: (REV, entity_id_cols) from source table's PK
				createAuditPrimaryKey( auditLogTable, transactionIdColumn, table.getPrimaryKey().getColumns() );
			}
			enableAudit( auditable, auditLogTable, transactionIdColumn, modificationTypeColumn );
			createRevisionForeignKey( auditLogTable, transactionIdColumn, context );
			addTransactionEndColumns( auditTable, auditable, auditLogTable, context );
		} );
	}

	private static void bindSecondaryAuditTables(
			Audited.@Nullable Table auditTable,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final String txIdColumnName;
		final String auditSchema;
		final String auditCatalog;
		if ( auditTable != null ) {
			txIdColumnName = auditTable.transactionIdColumn();
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
		}
		else {
			txIdColumnName = Audited.Table.DEFAULT_TRANSACTION_ID;
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
						txIdColumnName,
						resolveExcludedColumns( join.getProperties() ),
						nullIfBlank( auditSchema ),
						nullIfBlank( auditCatalog ),
						customName,
						context
				);
				createAuditTableForeignKey( secondaryAuditTable, rootClass.getEntityName(), rootClass.getAuxiliaryTable() );
				// Secondary tables only get tx-id (no mod type, no REVEND)
				join.setAuxiliaryTable( secondaryAuditTable );
				join.addAuxiliaryColumn( TRANSACTION_ID, secondaryAuditTable.getPrimaryKey().getColumn( 0 ) );
			}
		} );
	}

	private static void bindSubclassAuditTables(
			Audited.@Nullable Table auditTable,
			RootClass rootClass,
			MetadataBuildingContext context) {
		final String txIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			txIdColumnName = auditTable.transactionIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			txIdColumnName = Audited.Table.DEFAULT_TRANSACTION_ID;
			modTypeColumnName = Audited.Table.DEFAULT_MODIFICATION_TYPE;
		}
		// Defer to second pass since subclasses haven't been added to rootClass yet
		context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored ->
				bindSubclassAuditTables(
						rootClass,
						auditTable,
						txIdColumnName,
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
			String txIdColumnName,
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
						txIdColumnName,
						resolveExcludedColumns( subclass.getProperties() ),
						effective != null ? nullIfBlank( effective.schema() ) : null,
						effective != null ? nullIfBlank( effective.catalog() ) : null,
						effective != null ? nullIfBlank( effective.name() ) : null,
						context
				);
				subclass.addAuxiliaryColumn( TRANSACTION_ID, subclassAuditTable.getPrimaryKey().getColumn( 0 ) );
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
				bindSubclassAuditTables( subclass, auditTable, txIdColumnName, modTypeColumnName, context );
			}
		}
	}

	static void enableAudit(
			Stateful model, Table auditTable,
			Column transactionIdColumn, Column modificationTypeColumn) {
		model.setAuxiliaryTable( auditTable );
		model.addAuxiliaryColumn( TRANSACTION_ID, transactionIdColumn );
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
		final String txIdColumnName;
		final String modTypeColumnName;
		if ( auditTable != null ) {
			auditSchema = auditTable.schema();
			auditCatalog = auditTable.catalog();
			txIdColumnName = auditTable.transactionIdColumn();
			modTypeColumnName = auditTable.modificationTypeColumn();
		}
		else {
			auditSchema = "";
			auditCatalog = "";
			txIdColumnName = Audited.Table.DEFAULT_TRANSACTION_ID;
			modTypeColumnName = Audited.Table.DEFAULT_MODIFICATION_TYPE;
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
			final var transactionIdColumn = createAuditColumn(
					txIdColumnName,
					getTransactionIdType( context ),
					middleAuditTable,
					context
			);
			final var modificationTypeColumn = createAuditColumn(
					modTypeColumnName,
					Byte.class,
					middleAuditTable,
					context
			);
			middleAuditTable.addColumn( transactionIdColumn );
			middleAuditTable.addColumn( modificationTypeColumn );
			createAuditPrimaryKey( middleAuditTable, transactionIdColumn, keyColumns );
			createRevisionForeignKey( middleAuditTable, transactionIdColumn, context );
			enableAudit( collection, middleAuditTable, transactionIdColumn, modificationTypeColumn );
			addTransactionEndColumns( auditTable, collection, middleAuditTable, context );
		} );
	}

	static void bindRevisionEntity(
			RevisionEntity revisionEntity,
			RootClass rootClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();

		// todo : @RevisionEntity currently requires @Entity;
		//  could we automatically imply @Entity for @RevisionEntity classes
		//  so users don't need both annotations?

		// The entity must not be audited
		if ( classDetails.hasAnnotationUsage( Audited.class, modelsContext ) ) {
			throw new MappingException( "The @RevisionEntity entity cannot be audited" );
		}

		// Scan class members (including supertypes) for @TransactionId,
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
						RevisionEntity.TransactionId.class,
						classDetails
				);
				revTimestampMember = checkAnnotation(
						member,
						revTimestampMember,
						RevisionEntity.Timestamp.class,
						classDetails
				);
				modifiedEntityNamesMember = checkAnnotation(
						member,
						modifiedEntityNamesMember,
						RevisionEntity.ModifiedEntities.class,
						classDetails
				);
			}
			for ( var member : current.getMethods() ) {
				revNumberMember = checkAnnotation(
						member,
						revNumberMember,
						RevisionEntity.TransactionId.class,
						classDetails
				);
				revTimestampMember = checkAnnotation(
						member,
						revTimestampMember,
						RevisionEntity.Timestamp.class,
						classDetails
				);
				modifiedEntityNamesMember = checkAnnotation(
						member,
						modifiedEntityNamesMember,
						RevisionEntity.ModifiedEntities.class,
						classDetails
				);
			}
		}

		if ( revNumberMember == null ) {
			throw new MappingException(
					"@RevisionEntity '" + classDetails.getName()
							+ "' must have a property annotated with @RevisionEntity.TransactionId"
			);
		}
		if ( revTimestampMember == null ) {
			throw new MappingException(
					"@RevisionEntity '" + classDetails.getName()
							+ "' must have a property annotated with @RevisionEntity.Timestamp"
			);
		}

		// Configure the supplier eagerly
		final var serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final Class<? extends RevisionListener> listenerClass = revisionEntity.listener();
		final RevisionListener listener = listenerClass != RevisionListener.class
				? serviceRegistry.requireService( ManagedBeanRegistry.class ).getBean( listenerClass ).getBeanInstance()
				: null;
		final var supplier = new RevisionEntitySupplier<>(
				classDetails.toJavaClass(),
				revNumberMember.resolveAttributeName(),
				revTimestampMember.resolveAttributeName(),
				modifiedEntityNamesMember != null
						? modifiedEntityNamesMember.resolveAttributeName()
						: null, listener
		);
		final var revNumberType = revNumberMember.getType().determineRawClass().toJavaClass();
		serviceRegistry.requireService( TransactionIdentifierService.class )
				.contributeIdentifierSupplier( supplier, revNumberType );

		// Defer validation (basic type, mapped as Hibernate property) and
		// unique constraint to second pass when entity properties are fully bound
		final String entityName = rootClass.getEntityName();
		final String revNumberName = revNumberMember.resolveAttributeName();
		final String revTimestampName = revTimestampMember.resolveAttributeName();
		context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored ->
				validateRevisionEntity( entityName, revNumberName, revTimestampName, context )
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
						"@RevisionEntity '" + classDetails.getName()
								+ "' has multiple members annotated with @"
								+ annotationType.getSimpleName()
				);
			}
			return member;
		}
		return existing;
	}

	/**
	 * Second-pass validation: verify {@code @RevisionEntity.TransactionId}
	 * and {@code @RevisionEntity.Timestamp} are mapped as basic properties,
	 * and add a unique constraint on non-ID {@code @TransactionId}.
	 */
	private static void validateRevisionEntity(
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
				"@RevisionEntity.TransactionId"
		);
		requireBasicProperty( entityBinding, revTimestampName, "@RevisionEntity.Timestamp" );
		// Add unique constraint on non-ID @TransactionId
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
							+ "' is not mapped as a property on @RevisionEntity '"
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
	 * REV -> REVINFO FK (if a revision entity is configured).
	 */
	private static Table createAuditTable(
			Table sourceTable,
			String txIdColumnName,
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
		final var revColumn = createAuditColumn( txIdColumnName, getTransactionIdType( context ), auditTable, context );
		auditTable.addColumn( revColumn );
		createAuditPrimaryKey( auditTable, revColumn, sourceTable.getPrimaryKey().getColumns() );
		createRevisionForeignKey( auditTable, revColumn, context );
		return auditTable;
	}

	private static void createAuditPrimaryKey(
			Table auditTable,
			Column transactionIdColumn,
			Iterable<Column> sourceKeyColumns) {
		final var pk = new PrimaryKey( auditTable );
		pk.addColumn( transactionIdColumn );
		for ( var sourceCol : sourceKeyColumns ) {
			pk.addColumn( auditTable.getColumn( sourceCol ) );
		}
		auditTable.setPrimaryKey( pk );
	}

	private static Class<?> getTransactionIdType(MetadataBuildingContext context) {
		return context.getBootstrapContext().getServiceRegistry()
				.requireService( TransactionIdentifierService.class )
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
		final var value = context.getBootstrapContext().getServiceRegistry()
				.requireService( ConfigurationService.class )
				.getSetting( StateManagementSettings.AUDIT_STRATEGY, String.class, "default" );
		return "validity".equalsIgnoreCase( value );
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
						auditTableAnnotation != null ? auditTableAnnotation.transactionEndIdColumn() : Audited.Table.DEFAULT_TRANSACTION_END,
						getTransactionIdType( context ), auditTable, context );
		revEndColumn.setNullable( true );
		auditTable.addColumn( revEndColumn );
		holder.addAuxiliaryColumn( TRANSACTION_END, revEndColumn );
		createRevisionForeignKey( auditTable, revEndColumn, context );

		final String revEndTsName = auditTableAnnotation != null
				? auditTableAnnotation.transactionEndTimestampColumn()
				: "";
		if ( !isBlank( revEndTsName ) ) {
			final var revEndTsColumn = createAuditColumn( revEndTsName, Instant.class, auditTable, context );
			revEndTsColumn.setNullable( true );
			auditTable.addColumn( revEndTsColumn );
			holder.addAuxiliaryColumn( TRANSACTION_END_TIMESTAMP, revEndTsColumn );
		}
	}

	/**
	 * Create a FK from the audit table's REV (or REVEND) column to the
	 * revision entity's PK. Only applies when {@code @RevisionEntity}
	 * is configured.
	 */
	private static void createRevisionForeignKey(
			Table auditTable,
			Column revColumn,
			MetadataBuildingContext context) {
		final String revisionEntityName = getRevisionEntityName( context );
		if ( revisionEntityName != null ) {
			auditTable.createForeignKey(
					null,
					List.of( revColumn ),
					revisionEntityName,
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

	private static @Nullable String getRevisionEntityName(MetadataBuildingContext context) {
		final var supplier = RevisionEntitySupplier.resolve( context.getBootstrapContext().getServiceRegistry() );
		return supplier != null ? supplier.getRevisionEntityClass().getName() : null;
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
}
