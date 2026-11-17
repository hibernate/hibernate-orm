package org.hibernate.orm.test.generator.batch;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inserting two entities with {@code insertMultiple} and a sequence with {@code allocationSize = 1}
 * fetches both ids with a single sequence query. Without optimizer pooling, dialects that lack a
 * dedicated multi-value construct (e.g. SQL Server 2012 - 2019) must emulate this with
 * {@code select next value for s union all select next value for s}, which they reject.
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@DomainModel(annotatedClasses = BatchSequenceSupportTest.SequenceEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
class BatchSequenceSupportTest {

	@Test
	void insertTwoEntitiesWithAllocationSizeOne(SessionFactoryScope scope) {
		final SequenceEntity first = new SequenceEntity();
		final SequenceEntity second = new SequenceEntity();
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inStatelessTransaction( session -> session.insertMultiple( List.of( first, second ) ) );
		// both ids are fetched with one query, i.e. through the batch generation path
		assertThat( inspector.getSqlQueries() )
				.filteredOn( sql -> sql.toLowerCase( java.util.Locale.ROOT ).contains( "batch_seq" ) )
				.hasSize( 1 );
		assertThat( first.id ).isNotNull();
		assertThat( second.id ).isNotNull().isNotEqualTo( first.id );
	}

	@Entity(name = "SequenceEntity")
	static class SequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_seq_gen")
		@SequenceGenerator(name = "batch_seq_gen", sequenceName = "batch_seq", allocationSize = 1)
		Long id;
		String name;
	}
}
