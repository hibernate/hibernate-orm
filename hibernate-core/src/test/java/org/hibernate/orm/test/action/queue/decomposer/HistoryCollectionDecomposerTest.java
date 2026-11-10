package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.internal.decompose.collection.BasicCollectionDecomposer;
import org.hibernate.action.queue.internal.decompose.collection.HistoryCollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.annotations.Temporal;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises both row-planning paths directly and checks dependencies independently of
/// the graph scheduler's preferred ordering. No planned operation is executed here.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@Jpa(annotatedClasses = HistoryCollectionDecomposerTest.Owner.class, properties = {
		@Setting(name = "hibernate.flush.queue.type", value = "graph"),
		@Setting(name = "hibernate.temporal.table_strategy", value = "HISTORY_TABLE")
})
class HistoryCollectionDecomposerTest {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void valueChangesHaveHistoryDependencies(boolean fallback, EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var session = em.unwrap( SessionImplementor.class );
			var factory = session.getFactory();
			var persister = (BasicCollectionPersister) factory.getMappingMetamodel()
					.getCollectionDescriptor( Owner.class.getName() + ".values" );
			var owner = new Owner(); owner.id = 1;
			var collection = new InspectableList( session, fallback );
			collection.setOwner( owner );
			session.getPersistenceContextInternal().addInitializedCollection( persister, collection, 1 );
			collection.set( 0, "changed-one" );
			collection.set( 1, "changed-two" );
			// A null interpretation deliberately enters the retained snapshot/changeset
			// fallback in BasicCollectionDecomposer, independent of preparation policy.
			var action = new PreparedCollectionMutation( PreparedCollectionMutation.Kind.UPDATE,
					collection, new CollectionEndpoint( persister, 1 ), false, owner, 1, null );
			var operations = new ArrayList<FlushOperation>();
			new BasicCollectionDecomposer( persister, factory, new HistoryCollectionMutationPlanContributor() )
					.decomposeUpdate( action, 0, session, null, operations::add );
			assertThat( collection.changeSetRequested ).isTrue();
			assertThat( collection.snapshotFallbackUsed ).isEqualTo( fallback );
			String history = persister.getAttributeMapping().getTemporalMapping().getTableName();
			var ends = operations.stream().filter( op -> op.getTableExpression().equals( history )
					&& op.getKind() == MutationKind.UPDATE ).toList();
			var inserts = operations.stream().filter( op -> op.getTableExpression().equals( history )
					&& op.getKind() == MutationKind.INSERT ).toList();
			assertThat( ends ).hasSize( 2 ); assertThat( inserts ).hasSize( 2 );
			assertThat( ends.get( 1 ).getExecutionPrerequisite() ).isSameAs( ends.get( 0 ) );
			assertThat( inserts ).allSatisfy( op -> assertThat( op.getExecutionPrerequisite() ).isSameAs( ends.get( 1 ) ) );
			assertThat( operations ).hasSize( 6 );
			session.clear();
		} );
	}

	static class InspectableList extends PersistentList<String> {
		final boolean fallback;
		boolean changeSetRequested;
		boolean snapshotFallbackUsed;
		InspectableList(SessionImplementor session, boolean fallback) {
			super( session, new ArrayList<>( List.of( "one", "two" ) ) ); this.fallback = fallback;
		}
		@Override public CollectionChangeSet getChangeSet(CollectionPersister persister) {
			changeSetRequested = true;
			return fallback ? null : super.getChangeSet( persister );
		}
		@Override public boolean needsUpdating(Object entry, int i, org.hibernate.metamodel.mapping.PluralAttributeMapping attribute) {
			snapshotFallbackUsed = true;
			return super.needsUpdating( entry, i, attribute );
		}
	}

	@Entity(name = "HistoryPlannerOwner") @Temporal
	static class Owner {
		@Id int id;
		@ElementCollection @OrderColumn @Temporal
		List<String> values = new ArrayList<>();
	}
}
