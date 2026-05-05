/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.action.queue.internal.decompose.collection.AuditCollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.internal.decompose.entity.AuditEntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.audit.ModificationType;
import org.hibernate.mapping.AuxiliaryTableHolder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.AuditMappingImpl;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorAudit;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorAudit;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorAudit;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorAudit;
import org.hibernate.persister.entity.mutation.MergeCoordinatorAudit;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorAudit;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.persister.state.spi.StateManagementGraphIntegration;
import org.hibernate.persister.state.spi.StateManagementLegacyIntegration;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.boot.model.internal.AuditHelper.MODIFICATION_TYPE;
import static org.hibernate.boot.model.internal.AuditHelper.INVALIDATING_CHANGESET_ID;
import static org.hibernate.boot.model.internal.AuditHelper.CHANGESET_ID;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;
import static org.hibernate.persister.state.internal.AbstractStateManagement.resolveMutationTarget;

/**
 * State management for {@linkplain org.hibernate.annotations.Audited audited}
 * entities and collections.
 *
 * @author Gavin King
 * @since 7.4
 */
public class AuditStateManagement implements StateManagement, StateManagementLegacyIntegration {
	public static final AuditStateManagement INSTANCE = new AuditStateManagement();

	private final StateManagementLegacyIntegration standardLegacyIntegration =
			StandardStateManagement.INSTANCE.getLegacyIntegration();

	private final StateManagementGraphIntegration graphIntegration = new StateManagementGraphIntegration() {
		@Override
		public EntityMutationPlanContributor createEntityMutationPlanContributor(EntityPersister persister) {
			return new AuditEntityMutationPlanContributor( persister, persister.getFactory() );
		}

		@Override
		public CollectionMutationPlanContributor createCollectionMutationPlanContributor(CollectionPersister persister) {
			return new AuditCollectionMutationPlanContributor( persister, persister.getFactory() );
		}
	};

	private AuditStateManagement() {
	}

	@Override
	public StateManagementLegacyIntegration getLegacyIntegration() {
		return this;
	}

	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorAudit( persister, persister.getFactory(),
				standardLegacyIntegration.createInsertCoordinator( persister ) );
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return new UpdateCoordinatorAudit( persister, persister.getFactory(),
				standardLegacyIntegration.createUpdateCoordinator( persister ) );
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new MergeCoordinatorAudit( persister, persister.getFactory(),
				standardLegacyIntegration.createMergeCoordinator( persister ) );
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorAudit( persister, persister.getFactory(),
				standardLegacyIntegration.createDeleteCoordinator( persister ) );
	}

	@Override
	public StateManagementGraphIntegration getGraphIntegration() {
		return graphIntegration;
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !AbstractStateManagement.isInsertAllowed( persister ) ) {
			return new InsertRowsCoordinatorNoOp( mutationTarget );
		}
		else {
			return new InsertRowsCoordinatorAudit(
					mutationTarget,
					standardLegacyIntegration.createInsertRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory()
			);
		}
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
		// Collection audit rows are always ADD/DEL (never MOD).
		// The semantic diff in InsertRowsCoordinatorAudit handles all audit writes.
		return standardLegacyIntegration.createUpdateRowsCoordinator( persister );
	}

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
		// Collection audit rows are always ADD/DEL (never MOD).
		// The semantic diff in InsertRowsCoordinatorAudit handles all audit writes.
		return standardLegacyIntegration.createDeleteRowsCoordinator( persister );
	}

	@Override
	public RemoveCoordinator createRemoveCoordinator(CollectionPersister persister) {
		if ( !persister.needsRemove() ) {
			return new RemoveCoordinatorNoOp( resolveMutationTarget( persister ) );
		}
		return new RemoveCoordinatorAudit(
				resolveMutationTarget( persister ),
				standardLegacyIntegration.createRemoveCoordinator( persister ),
				persister.getIndexColumnIsSettable(),
				persister.getElementColumnIsSettable(),
				persister.getIndexIncrementer(),
				persister.getFactory()
		);
	}

	@Override
	public AuditMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass rootClass,
			MappingModelCreationProcess creationProcess) {
		final var aep = (AbstractEntityPersister) persister;
		final var tableAuditInfoMap = buildTableAuditInfoMap( rootClass, aep, creationProcess );
		return new AuditMappingImpl( tableAuditInfoMap, persister, creationProcess );
	}

	private static Map<String, AuditMappingImpl.TableAuditInfo> buildTableAuditInfoMap(
			RootClass rootClass,
			AbstractEntityPersister persister,
			MappingModelCreationProcess creationProcess) {
		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var changesetCoordinator =
				creationContext.getSessionFactory()
						.getChangesetCoordinator();
		final var csIdJdbcMapping =
				resolveJdbcMapping( typeConfiguration,
						changesetCoordinator.getIdentifierType() );
		final var modTypeJdbcMapping = resolveJdbcMapping( typeConfiguration, ModificationType.class );

		final var map = new HashMap<String, AuditMappingImpl.TableAuditInfo>();

		// Root table entry
		addTableAuditInfo(
				map,
				rootClass.getTable(),
				rootClass.getAuxiliaryTable(),
				rootClass,
				persister,
				csIdJdbcMapping,
				modTypeJdbcMapping,
				creationProcess
		);

		// For TABLE_PER_CLASS, prepare audit subquery generation context
		// (the tableNameResolver lambda captures the map, so it resolves lazily)
		final Function<String, String> tableNameResolver;
		final List<String> extraColumns;
		if ( persister instanceof UnionSubclassEntityPersister ) {
			tableNameResolver = originalName -> map.get( originalName ).auditTableName();
			final var rootInfo = map.values().iterator().next();
			final var extras = new ArrayList<>( List.of(
					rootInfo.changesetIdMapping().getSelectionExpression(),
					rootInfo.modificationTypeMapping().getSelectionExpression()
			) );
			if ( rootInfo.invalidatingChangesetMapping() != null ) {
				extras.add( rootInfo.invalidatingChangesetMapping().getSelectionExpression() );
			}
			extraColumns = extras;
		}
		else {
			tableNameResolver = null;
			extraColumns = null;
		}

		// Secondary table entries (@SecondaryTable joins)
		for ( var join : rootClass.getJoins() ) {
			if ( join.getAuxiliaryTable() != null ) {
				addTableAuditInfo(
						map,
						join.getTable(),
						join.getAuxiliaryTable(),
						join,
						persister,
						csIdJdbcMapping,
						modTypeJdbcMapping,
						creationProcess
				);
			}
		}

		// Subclass table entries (JOINED / TABLE_PER_CLASS)
		for ( var subclass : rootClass.getSubclasses() ) {
			if ( subclass.getAuxiliaryTable() != null ) {
				addTableAuditInfo(
						map,
						subclass.getTable(),
						subclass.getAuxiliaryTable(),
						subclass,
						persister,
						csIdJdbcMapping,
						modTypeJdbcMapping,
						creationProcess
				);
			}
			// For TABLE_PER_CLASS intermediate classes, build audit subquery inline
			// (getSubclasses() is depth-first, so subtypes' entries are already in the map)
			if ( tableNameResolver != null && subclass.hasSubclasses() ) {
				addAuditSubquery( map, subclass, tableNameResolver, extraColumns, creationProcess );
			}
		}

		// Root's audit subquery (needs all subclass entries, so must come last)
		if ( tableNameResolver != null && rootClass.hasSubclasses() ) {
			addAuditSubquery( map, rootClass, tableNameResolver, extraColumns, creationProcess );
		}

		return map;
	}

	private static void addAuditSubquery(
			Map<String, AuditMappingImpl.TableAuditInfo> map,
			PersistentClass bootClass,
			Function<String, String> tableNameResolver,
			List<String> extraColumns,
			MappingModelCreationProcess creationProcess) {
		assert !bootClass.getSubclasses().isEmpty();
		final var unionPersister = (UnionSubclassEntityPersister)
				creationProcess.getEntityPersister( bootClass.getEntityName() );
		final var rootInfo = map.values().iterator().next();
		final String originalSubquery = unionPersister.getTableName();
		final String auditSubquery = unionPersister.generateSubquery(
				bootClass,
				tableNameResolver,
				extraColumns
		);
		map.put(
				originalSubquery,
				new AuditMappingImpl.TableAuditInfo(
						auditSubquery,
						rootInfo.changesetIdMapping(),
						rootInfo.modificationTypeMapping(),
						rootInfo.invalidatingChangesetMapping()
				)
		);
	}

	private static void addTableAuditInfo(
			Map<String, AuditMappingImpl.TableAuditInfo> map,
			Table originalTable,
			Table auditTable,
			AuxiliaryTableHolder holder,
			AbstractEntityPersister persister,
			JdbcMapping csIdJdbcMapping,
			JdbcMapping modTypeJdbcMapping,
			MappingModelCreationProcess creationProcess) {
		final String originalTableName = persister.determineTableName( originalTable );
		final String auditTableName = persister.determineTableName( auditTable );
		map.put(
				originalTableName,
				createTableAuditInfo( auditTableName, holder, csIdJdbcMapping, modTypeJdbcMapping, creationProcess )
		);
	}

	private static AuditMappingImpl.TableAuditInfo createTableAuditInfo(
			String auditTableName,
			AuxiliaryTableHolder holder,
			JdbcMapping csIdJdbcMapping,
			JdbcMapping modTypeJdbcMapping,
			MappingModelCreationProcess creationProcess) {
		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		return new AuditMappingImpl.TableAuditInfo(
				auditTableName,
				toSelectableMapping(
						auditTableName,
						holder.getAuxiliaryColumn( CHANGESET_ID ),
						csIdJdbcMapping,
						creationProcess
				),
				toSelectableMapping(
						auditTableName,
						holder.getAuxiliaryColumn( MODIFICATION_TYPE ),
						modTypeJdbcMapping,
						creationProcess
				),
				toSelectableMapping(
						auditTableName,
						holder.getAuxiliaryColumn( INVALIDATING_CHANGESET_ID ),
						csIdJdbcMapping,
						creationProcess
				)
		);
	}

	private static @Nullable SelectableMapping toSelectableMapping(
			String tableName,
			@Nullable Column column,
			JdbcMapping jdbcMapping,
			MappingModelCreationProcess creationProcess) {
		if ( column == null ) {
			return null;
		}
		final var creationContext = creationProcess.getCreationContext();
		return SelectableMappingImpl.from(
				tableName,
				column,
				jdbcMapping,
				creationContext.getTypeConfiguration(),
				true,
				false,
				false,
				creationContext.getDialect(),
				creationContext.getSessionFactory().getQueryEngine().getSqmFunctionRegistry(),
				creationContext
		);
	}

	private static JdbcMapping resolveJdbcMapping(TypeConfiguration typeConfiguration, Class<?> javaType) {
		final var basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		return basicType != null ? basicType : typeConfiguration.standardBasicTypeForJavaType( javaType );
	}

	@Override
	public AuditMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final var auditTable = bootDescriptor.getAuxiliaryTable();
		if ( auditTable == null ) {
			// No audit table for this collection (e.g. @OneToMany @JoinColumn --
			// the child entity's audit table handles FK auditing)
			return null;
		}
		final String originalTableName = getTableIdentifierExpression(
				bootDescriptor.getCollectionTable(), creationProcess );
		final String auditTableName = getTableIdentifierExpression( auditTable, creationProcess );
		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var changesetCoordinator =
				creationContext.getSessionFactory()
						.getChangesetCoordinator();
		final var csIdJdbcMapping =
				resolveJdbcMapping( typeConfiguration,
						changesetCoordinator.getIdentifierType() );
		final var modTypeJdbcMapping = resolveJdbcMapping( typeConfiguration, ModificationType.class );
		return new AuditMappingImpl(
				Map.of(
						originalTableName,
						createTableAuditInfo(
								auditTableName,
								bootDescriptor,
								csIdJdbcMapping,
								modTypeJdbcMapping,
								creationProcess
						)
				),
				null,
				creationProcess
		);
	}

}
