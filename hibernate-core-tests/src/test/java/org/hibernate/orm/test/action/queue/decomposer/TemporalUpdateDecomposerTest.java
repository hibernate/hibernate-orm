package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.TenantId;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests graph decomposition for temporal updates.
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = TemporalUpdateDecomposerTest.TemporalEntity.class,
		properties = {
				@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
				@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
		}
)
public class TemporalUpdateDecomposerTest {

	@Test
	public void testTemporalUpdateDecomposesToEndAndInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final TemporalEntity entity = new TemporalEntity();
			entity.id = 1L;
			entity.name = "before";
			entityManager.persist( entity );
		} );
		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final var entity = entityManager.find( TemporalEntity.class, 1L );

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( TemporalEntity.class );

			final Object[] previousState = persister.getValues( entity );
			entity.name = "after";
			final EntityUpdateAction action = createUpdateAction( entity, previousState, session, persister );
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 2, operations.size() );
			assertEquals( MutationKind.UPDATE, operations.get( 0 ).getKind() );
			assertEquals( MutationKind.INSERT, operations.get( 1 ).getKind() );

			entity.name = "before";
		} );
	}

	@Test
	@Jpa(annotatedClasses = TenantItem.class, properties = {
			@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
			@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
					value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
			@Setting(name = MultiTenancySettings.MULTI_TENANT_RLS_ENABLED, value = "false")
	})
	void tenantOwnershipCheckPreservesHistoryPrerequisite(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final var item = new TenantItem();
			item.id = 1L;
			item.name = "before";
			em.persist( item );
		} );
		scope.inTransaction( em -> {
			final var session = em.unwrap( SessionImplementor.class );
			final var item = em.find( TenantItem.class, 1L );

			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( TenantItem.class );
			final var previous = persister.getValues( item );
			item.name = "after";
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( createUpdateAction( item, previous, session, persister ),
					0, session, null, operations::add );

			final var currentTable = persister.getIdentifierTableDetails().getTableName();
			final var historyTable = persister.getTemporalMapping().getTableName();
			final var ownershipCheck = operation( operations, currentTable, MutationKind.UPDATE );
			final var closeHistory = operation( operations, historyTable, MutationKind.UPDATE );
			final var insertHistory = operation( operations, historyTable, MutationKind.INSERT );
			assertEquals( List.of( ownershipCheck ), closeHistory.getExecutionPrerequisites() );
			assertEquals( List.of( closeHistory, ownershipCheck ), insertHistory.getExecutionPrerequisites() );
			item.name = "before";
		} );
	}

	private static FlushOperation operation(List<FlushOperation> operations, String table, MutationKind kind) {
		return operations.stream().filter( op -> op.getKind() == kind && table.equals( op.getTableExpression() ) )
				.findFirst().orElseThrow();
	}

	private static EntityUpdateAction createUpdateAction(
			Object entity,
			Object[] previousState,
			SessionImplementor session,
			EntityPersister persister) {
		return new EntityUpdateAction(
				persister.getIdentifier( entity, session ),
				persister.getValues( entity ),
				new int[] { persister.findAttributeMapping( "name" ).getStateArrayPosition() },
				false,
				previousState,
				persister.getVersion( entity ),
				null,
				entity,
				null,
				persister,
				(EventSource) session
		);
	}

	@Entity(name = "GraphTemporalUpdateEntity")
	@Table(name = "graph_temporal_update_entity")
	@org.hibernate.annotations.Temporal
	public static class TemporalEntity {
		@Id
		Long id;

		String name;
	}

	@Entity(name = "TenantItem")
	@Table(name = "tenant_item")
	@SecondaryTable(name = "tenant_item_details")
	@Temporal
	static class TenantItem {
		@Id Long id;
		@TenantId String tenant;
		String name;
		@Column(table = "tenant_item_details") String detail;
	}
}
