package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Nonnull;
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

import jakarta.annotation.Nullable;

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
		bindAuditTable(
				auditTable,
				rootClass,
				auditTable == null ? null : auditTable.name(),
				auditTable == null ? null : auditTable.schema(),
				auditTable == null ? null : auditTable.catalog(),
				context
		);
		bindSubclassAuditTables( auditTable, rootClass, context );
		bindSecondaryAuditTables( auditTable, rootClass, classDetails, context );
	}

	/**
	 * Binds the audit table of a collection, or a middle audit table when given the entity
	 * referenced by a unidirectional {@code @OneToMany} join column. The column names come
	 * from the root's {@code @Audited.Table}, and the schema and catalog from the owning
	 * entity's, but not its table name. Each nonblank member of the {@code @Audited.Override}
	 * collection table wins over the member of the property's collection table.
	 */
	static void bindCollectionAuditTable(
			Collection collection,
			@Nullable String oneToManyEntityName,
			@Nullable Audited.CollectionTable overrideAuditTable,
			@Nullable Audited.CollectionTable collectionAuditTable,
			MetadataBuildingContext context) {
		final var owner = collection.getOwner();
		final var rootDetails = findClassDetails( owner.getRootClass(), context );
		final var auditTable =
				rootDetails == null ? null : EntityBinder.extract( Audited.Table.class, rootDetails, context );
		final var ownerAuditTable = entityAuditTable( owner, auditTable, context );
		final String name =
				collectionTableMember( Audited.CollectionTable::name, overrideAuditTable, collectionAuditTable, null );
		final String schema = collectionTableMember(
				Audited.CollectionTable::schema,
				overrideAuditTable,
				collectionAuditTable,
				ownerAuditTable == null ? null : ownerAuditTable.schema()
		);
		final String catalog = collectionTableMember(
				Audited.CollectionTable::catalog,
				overrideAuditTable,
				collectionAuditTable,
				ownerAuditTable == null ? null : ownerAuditTable.catalog()
		);
		if ( oneToManyEntityName == null ) {
			bindAuditTable( auditTable, collection, name, schema, catalog, context );
		}
		else {
			bindOneToManyAuditTable( auditTable, collection, oneToManyEntityName, name, schema, catalog, context );
		}
	}

	private static void bindAuditTable(
			@Nullable Audited.Table auditTable,
			Stateful auditable,
			@Nullable String explicitAuditTableName,
			@Nullable String auditSchema,
			@Nullable String auditCatalog,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final var table = auditable.getMainTable();
		final String csIdColumnName =
				auditTable == null ? DEFAULT_CHANGESET_ID_COLUMN_NAME : auditTable.changesetIdColumn();
		final String modTypeColumnName =
				auditTable == null ? DEFAULT_MODIFICATION_TYPE_COLUMN_NAME : auditTable.modificationTypeColumn();
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
		auditable.setStateManagementType( AuditStateManagement.class );

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
					? resolveExcludedColumns( rootClass, table )
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
			for ( var join : rootClass.getSubclassJoinClosure() ) {
				final var owner = join.getPersistentClass();
				final var ownerDetails = owner == rootClass ? null : findClassDetails( owner, context );
				final Map<String, String> ownerTableNames = new HashMap<>( secondaryAuditTableNames );
				if ( ownerDetails != null ) {
					ownerDetails.forEachAnnotationUsage(
							Audited.SecondaryTable.class,
							context.getBootstrapContext().getModelsContext(),
							sat -> ownerTableNames.put( sat.secondaryTableName(), sat.secondaryAuditTableName() )
					);
				}
				final var sourceTable = join.getTable();
				final String customName = ownerTableNames.get( sourceTable.getName() );
				final var secondaryAuditTable = createAuditTable(
						sourceTable,
						csIdColumnName,
						resolveExcludedColumns( owner, sourceTable ),
						nullIfBlank( auditSchema ),
						nullIfBlank( auditCatalog ),
						customName,
						context
				);
				createAuditTableForeignKey(
						secondaryAuditTable,
						owner.getEntityName(),
						owner.getAuxiliaryTable() != null ? owner.getAuxiliaryTable() : rootClass.getAuxiliaryTable()
				);
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
		for ( var subclass : parent.getDirectSubclasses() ) {
			if ( subclass instanceof TableOwner ) {
				final var effective = entityAuditTable( subclass, auditTable, context );
				final var subclassAuditTable = createAuditTable(
						subclass.getTable(),
						csIdColumnName,
						resolveExcludedColumns( subclass, subclass.getTable() ),
						effective != null ? nullIfBlank( effective.schema() ) : null,
						effective != null ? nullIfBlank( effective.catalog() ) : null,
						// Only the subclass's own @Audited.Table names its audit table, never the root's
						effective != null && effective != auditTable ? nullIfBlank( effective.name() ) : null,
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
	}

	/**
	 * Create a middle audit table for unidirectional @OneToMany @JoinColumn.
	 * The table tracks collection membership with (parent_key, child_key, REV, REVTYPE)
	 * <p>
	 * The child entity's FK column is on the child table, but from an entity model
	 * perspective the collection is part of the parent entity's state.
	 */
	private static void bindOneToManyAuditTable(
			@Nullable Audited.Table auditTable,
			Collection collection,
			String referencedEntityName,
			@Nullable String explicitName,
			@Nullable String schema,
			@Nullable String catalog,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final var owner = collection.getOwner();
		final var referencedEntity = collector.getEntityBinding( referencedEntityName );
		final String csIdColumnName =
				auditTable == null ? DEFAULT_CHANGESET_ID_COLUMN_NAME : auditTable.changesetIdColumn();
		final String modTypeColumnName =
				auditTable == null ? DEFAULT_MODIFICATION_TYPE_COLUMN_NAME : auditTable.modificationTypeColumn();
		final var middleAuditTable = collector.addTable(
				isBlank( schema ) ? owner.getTable().getSchema() : schema,
				isBlank( catalog ) ? owner.getTable().getCatalog() : catalog,
				explicitName != null
						? explicitName
						: owner.getJpaEntityName() + "_" + referencedEntity.getJpaEntityName() + DEFAULT_TABLE_SUFFIX,
				null,
				false,
				context,
				false
		);
		collection.setStateManagementType( AuditStateManagement.class );
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
							auditTableAnnotation == null
									? DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME
									: auditTableAnnotation.invalidatingChangesetIdColumn(),
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

	/**
	 * The {@code @Audited.Table} determining the schema and catalog of the audit table
	 * holding the state of the given entity: the annotation declared directly by the
	 * entity owning its table (the entity itself, or its nearest superclass owning a
	 * table for a SINGLE_TABLE subclass), otherwise the root's. Annotations of other
	 * superclasses do not apply.
	 */
	private static @Nullable Audited.Table entityAuditTable(
			PersistentClass entity,
			@Nullable Audited.Table rootAuditTable,
			MetadataBuildingContext context) {
		var tableOwner = entity;
		while ( !( tableOwner instanceof TableOwner ) ) {
			tableOwner = tableOwner.getSuperclass();
		}
		final var classDetails = findClassDetails( tableOwner, context );
		final var ownAuditTable =
				classDetails == null ? null : classDetails.getDirectAnnotationUsage( Audited.Table.class );
		return ownAuditTable == null ? rootAuditTable : ownAuditTable;
	}

	private static @Nullable ClassDetails findClassDetails(PersistentClass entity, MetadataBuildingContext context) {
		final var className = entity.getClassName();
		return className == null
				? null
				: context.getBootstrapContext().getModelsContext().getClassDetailsRegistry()
						.findClassDetails( className );
	}

	private static @Nullable String collectionTableMember(
			Function<Audited.CollectionTable, String> member,
			@Nullable Audited.CollectionTable overrideAuditTable,
			@Nullable Audited.CollectionTable collectionAuditTable,
			@Nullable String defaultValue) {
		if ( overrideAuditTable != null && !isBlank( member.apply( overrideAuditTable ) ) ) {
			return member.apply( overrideAuditTable );
		}
		else if ( collectionAuditTable != null && !isBlank( member.apply( collectionAuditTable ) ) ) {
			return member.apply( collectionAuditTable );
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * The nearest {@code @Audited.Override} of the named property declared by the owner
	 * entity or its mapped superclasses.
	 */
	static @Nullable Audited.Override findAuditOverride(
			String propertyName,
			PersistentClass owner,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var superEntity = owner.getSuperclass();
		for ( var current = findClassDetails( owner, context );
				current != null && ( superEntity == null || !current.getName().equals( superEntity.getClassName() ) );
				current = current.getSuperClass() ) {
			final var override = current.getNamedAnnotationUsage(
					Audited.Override.class,
					propertyName,
					"name",
					modelsContext
			);
			if ( override != null ) {
				return override;
			}
		}
		return null;
	}

	/**
	 * Validates the {@code @Audited.Override}s of an entity of an audited hierarchy and of
	 * its mapped superclasses, and applies the nearest override of each property to its
	 * boot model. The search stops at the nearest entity superclass, whose overrides were
	 * applied when it was bound, so that an override may only target a property the entity
	 * owns.
	 */
	static void applyAuditOverrides(
			PersistentClass persistentClass,
			ClassDetails classDetails,
			MetadataBuildingContext context) {
		if ( persistentClass.getRootClass().getStateManagementType() != AuditStateManagement.class ) {
			// Only collection overrides take effect here (see CollectionBinder), and nothing is
			// validated, since a mapped superclass may be shared with audited entities
			return;
		}
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var superEntity = persistentClass.getSuperclass();
		final Set<String> applied = new HashSet<>();
		for ( var current = classDetails;
				current != null && ( superEntity == null || !current.getName().equals( superEntity.getClassName() ) );
				current = current.getSuperClass() ) {
			final var declaringClass = current;
			final Set<String> declared = new HashSet<>();
			current.forEachAnnotationUsage( Audited.Override.class, modelsContext, override -> {
				final String name = override.name();
				if ( isBlank( name ) ) {
					throw new MappingException( "Blank @Audited.Override property name '" + name
							+ "' on class '" + declaringClass.getName() + "'" );
				}
				if ( !declared.add( name ) ) {
					throw new MappingException( "Duplicate @Audited.Override of property '" + name
							+ "' on class '" + declaringClass.getName() + "'" );
				}
				if ( !persistentClass.hasProperty( name ) ) {
					throw new MappingException( "Unknown @Audited.Override property '" + name
							+ "' on class '" + declaringClass.getName() + "'" );
				}
				final var property = persistentClass.getProperty( name );
				if ( property == persistentClass.getIdentifierProperty() || property == persistentClass.getVersion() ) {
					throw new MappingException( "@Audited.Override of identifier or version property '" + name
							+ "' on class '" + declaringClass.getName() + "'" );
				}
				// The property of an entity superclass is shared with it and its other subclasses.
				// A generic specialization is the entity's copy of a generic property of its superclass.
				final var owner = property.isGenericSpecialization()
						? persistentClass.getSuperclass()
						: property.getPersistentClass();
				if ( owner != persistentClass ) {
					throw new MappingException( "@Audited.Override of property '" + name
							+ "' on class '" + declaringClass.getName() + "' inherited from entity '"
							+ owner.getEntityName() + "'" );
				}
				if ( applied.add( name ) ) {
					property.setAuditedExcluded( !override.isAudited() );
					if ( !override.isAudited() ) {
						// Like @Audited.Excluded
						property.setOptimisticLocked( false );
					}
				}
			} );
		}
	}

	private static Set<String> resolveExcludedColumns(PersistentClass owner, Table table) {
		final Set<String> auditedColumns = new HashSet<>();
		collectPropertyColumns( owner, table, auditedColumns );
		if ( !( owner instanceof UnionSubclass ) ) {
			for ( var subclass : owner.getSubclasses() ) {
				if ( !( subclass instanceof UnionSubclass ) ) {
					collectPropertyColumns( subclass, table, auditedColumns );
				}
			}
		}
		for ( var column : table.getPrimaryKey().getColumns() ) {
			auditedColumns.add( column.getCanonicalName() );
		}
		final var rootClass = owner.getRootClass();
		if ( table == rootClass.getTable() && rootClass.getDiscriminator() != null ) {
			for ( var column : rootClass.getDiscriminator().getColumns() ) {
				auditedColumns.add( column.getCanonicalName() );
			}
		}
		final Set<String> excluded = new HashSet<>();
		for ( var column : table.getColumns() ) {
			if ( !auditedColumns.contains( column.getCanonicalName() ) ) {
				excluded.add( column.getCanonicalName() );
			}
		}
		return excluded;
	}

	private static void collectPropertyColumns(
			PersistentClass persistentClass,
			Table table,
			Set<String> auditedColumns) {
		for ( var property : persistentClass.getPropertyClosure() ) {
			if ( mapsTable( persistentClass, property, table )
					&& !( property instanceof Backref )
					&& !property.isAuditedExcluded() ) {
				for ( var column : property.getColumns() ) {
					auditedColumns.add( column.getCanonicalName() );
				}
			}
		}
	}

	private static boolean mapsTable(PersistentClass persistentClass, Property property, Table table) {
		// A TABLE_PER_CLASS table holds the columns of every property mapped to a table of its
		// hierarchy, including generic specializations, whose value keeps the superclass table
		return property.getValue().getTable() == table
				|| persistentClass instanceof UnionSubclass && persistentClass.getTable() == table
						&& persistentClass.getTableClosure().contains( property.getValue().getTable() );
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
