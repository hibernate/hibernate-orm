package org.hibernate.orm.test.generator.batch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Nonnull;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.generator.GeneratorCreationContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(annotatedClasses = {
		BatchGenerationTest.BatchEntity.class,
		BatchGenerationTest.MixedEntity.class,
})
class BatchGenerationTest {

	@BeforeEach
	void resetCounters() {
		BatchStringGenerator.generateCallCount.set( 0 );
		BatchStringGenerator.generateBatchCallCount.set( 0 );
		BatchStringGenerator.totalRequestCount.set( 0 );
		NonBatchStringGenerator.generateCallCount.set( 0 );
		CountingIdGenerator.generateCallCount.set( 0 );
		CountingIdGenerator.generateBatchCallCount.set( 0 );
	}

	@AfterAll
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMultiEntityBatchGeneration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 5; i++ ) {
				session.persist( new BatchEntity( (long) ( i + 1 ), "entity-" + i ) );
			}
		} );

		assertThat( BatchStringGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be called once for 5 entities" )
				.isEqualTo( 1 );
		assertThat( BatchStringGenerator.totalRequestCount.get() )
				.as( "batch request should contain 5 items" )
				.isEqualTo( 5 );
		assertThat( BatchStringGenerator.generateCallCount.get() )
				.as( "generate() should not be called when batching" )
				.isEqualTo( 0 );

		scope.inTransaction( session -> {
			for ( long i = 1; i <= 5; i++ ) {
				final BatchEntity entity = session.find( BatchEntity.class, i );
				assertThat( entity.getBatchValue() )
						.as( "batch generated value should be non-null" )
						.isNotNull()
						.startsWith( "batch-" );
			}
		} );
	}

	@Test
	void testSingleEntityFallsBackToBatch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BatchEntity( 100L, "single" ) );
		} );

		assertThat( BatchStringGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be called even for a single entity" )
				.isEqualTo( 1 );
		assertThat( BatchStringGenerator.totalRequestCount.get() )
				.isEqualTo( 1 );

		scope.inTransaction( session -> {
			final BatchEntity entity = session.find( BatchEntity.class, 100L );
			assertThat( entity.getBatchValue() ).startsWith( "batch-" );
		} );
	}

	@Test
	void testMixedBatchAndNonBatchGenerators(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 3; i++ ) {
				session.persist( new MixedEntity( (long) ( i + 200 ), "mixed-" + i ) );
			}
		} );

		assertThat( BatchStringGenerator.generateBatchCallCount.get() )
				.as( "batch generator should be called once for 3 entities" )
				.isEqualTo( 1 );
		assertThat( BatchStringGenerator.totalRequestCount.get() )
				.isEqualTo( 3 );
		assertThat( NonBatchStringGenerator.generateCallCount.get() )
				.as( "non-batch generator should be called once per entity" )
				.isEqualTo( 3 );

		scope.inTransaction( session -> {
			for ( long i = 200; i < 203; i++ ) {
				final MixedEntity entity = session.find( MixedEntity.class, i );
				assertThat( entity.getBatchValue() ).startsWith( "batch-" );
				assertThat( entity.getNonBatchValue() ).startsWith( "nonbatch-" );
			}
		} );
	}

	@Test
	void testUpdatePathBatchGeneration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 3; i++ ) {
				session.persist( new BatchEntity( (long) ( i + 300 ), "update-" + i ) );
			}
		} );

		resetCounters();

		scope.inTransaction( session -> {
			for ( long i = 300; i < 303; i++ ) {
				final BatchEntity entity = session.find( BatchEntity.class, i );
				entity.setName( "updated-" + i );
			}
		} );

		assertThat( BatchStringGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be called once for update of 3 entities" )
				.isEqualTo( 1 );
		assertThat( BatchStringGenerator.totalRequestCount.get() )
				.isEqualTo( 3 );
	}

	@Test
	void testGeneratedValuesAreDistinct(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 10; i++ ) {
				session.persist( new BatchEntity( (long) ( i + 400 ), "distinct-" + i ) );
			}
		} );

		scope.inTransaction( session -> {
			final var values = new java.util.HashSet<String>();
			for ( long i = 400; i < 410; i++ ) {
				final BatchEntity entity = session.find( BatchEntity.class, i );
				values.add( entity.getBatchValue() );
			}
			assertThat( values ).hasSize( 10 );
		} );
	}

	// --- hibernate.id.batch_generation ---

	@Test
	@DomainModel(annotatedClasses = CountingIdEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.ID_BATCH_GENERATION, value = "true"))
	void testStatelessInsertMultipleIdBatchGenerationEnabled(SessionFactoryScope scope) {
		statelessInsertMultiple( scope, true );
	}

	@Test
	@DomainModel(annotatedClasses = CountingIdEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.ID_BATCH_GENERATION, value = "false"))
	void testStatelessInsertMultipleIdBatchGenerationDisabled(SessionFactoryScope scope) {
		statelessInsertMultiple( scope, false );
	}

	@Test
	@DomainModel(annotatedClasses = CountingIdEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.ID_BATCH_GENERATION, value = "true"))
	void testHqlInsertValuesIdBatchGenerationEnabled(SessionFactoryScope scope) {
		hqlInsertValues( scope, true );
	}

	@Test
	@DomainModel(annotatedClasses = CountingIdEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.ID_BATCH_GENERATION, value = "false"))
	void testHqlInsertValuesIdBatchGenerationDisabled(SessionFactoryScope scope) {
		hqlInsertValues( scope, false );
	}

	@Test
	@DomainModel(annotatedClasses = CountingIdEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = AvailableSettings.ID_BATCH_GENERATION, value = "false"))
	void testPersistIdBatchGenerationDisabled(SessionFactoryScope scope) {
		// control: stateful persist honors the setting
		persist( scope );
	}

	private void statelessInsertMultiple(SessionFactoryScope scope, boolean batchGeneration) {
		scope.inStatelessTransaction( session ->
				session.insertMultiple( List.of( new CountingIdEntity(), new CountingIdEntity() ) ) );
		assertIdGeneration( batchGeneration );
	}

	private void hqlInsertValues(SessionFactoryScope scope, boolean batchGeneration) {
		scope.inTransaction( session -> session.createMutationQuery(
				"insert into CountingIdEntity (name) values ('a'), ('b')" ).executeUpdate() );
		assertIdGeneration( batchGeneration );
	}

	private void persist(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CountingIdEntity() );
			session.persist( new CountingIdEntity() );
		} );
		assertIdGeneration( false );
	}

	private static void assertIdGeneration(boolean batchGeneration) {
		if ( batchGeneration ) {
			assertThat( CountingIdGenerator.generateBatchCallCount.get() ).as( "generateBatch calls" ).isEqualTo( 1 );
			assertThat( CountingIdGenerator.generateCallCount.get() ).as( "generate calls" ).isEqualTo( 0 );
		}
		else {
			assertThat( CountingIdGenerator.generateBatchCallCount.get() ).as( "generateBatch calls" ).isEqualTo( 0 );
			assertThat( CountingIdGenerator.generateCallCount.get() ).as( "generate calls" ).isEqualTo( 2 );
		}
	}

	// --- Entities ---

	@Entity(name = "BatchEntity")
	static class BatchEntity {
		@Id
		private Long id;
		private String name;
		@BatchGenerated
		private String batchValue;

		public BatchEntity() {
		}

		public BatchEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getBatchValue() {
			return batchValue;
		}
	}

	@Entity(name = "MixedEntity")
	static class MixedEntity {
		@Id
		private Long id;
		private String name;
		@BatchGenerated
		private String batchValue;
		@NonBatchGenerated
		private String nonBatchValue;

		public MixedEntity() {
		}

		public MixedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getBatchValue() {
			return batchValue;
		}

		public String getNonBatchValue() {
			return nonBatchValue;
		}
	}

	@Entity(name = "CountingIdEntity")
	static class CountingIdEntity {
		@Id
		@CountingId
		Long id;
		String name;
	}

	// --- Annotations ---

	@IdGeneratorType(CountingIdGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface CountingId {
	}

	@ValueGenerationType(generatedBy = BatchStringGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface BatchGenerated {
	}

	@ValueGenerationType(generatedBy = NonBatchStringGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface NonBatchGenerated {
	}

	// --- Generators ---

	public static class BatchStringGenerator implements BeforeExecutionGenerator {
		static final AtomicInteger generateCallCount = new AtomicInteger();
		static final AtomicInteger generateBatchCallCount = new AtomicInteger();
		static final AtomicInteger totalRequestCount = new AtomicInteger();

		private final AtomicInteger sequence = new AtomicInteger();

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			generateCallCount.incrementAndGet();
			return "batch-" + sequence.incrementAndGet();
		}

		@Override
		public boolean supportsBatchGeneration() {
			return true;
		}

		@Override
		public @Nonnull Object[] generateBatch(
				@Nonnull SharedSessionContractImplementor session,
				@Nonnull GenerationRequests requests,
				@Nonnull EventType eventType) {
			generateBatchCallCount.incrementAndGet();
			totalRequestCount.addAndGet( requests.size() );
			final Object[] results = new Object[requests.size()];
			for ( int i = 0; i < requests.size(); i++ ) {
				results[i] = "batch-" + sequence.incrementAndGet();
			}
			return results;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_AND_UPDATE;
		}
	}

	public static class NonBatchStringGenerator implements BeforeExecutionGenerator {
		static final AtomicInteger generateCallCount = new AtomicInteger();

		private final AtomicInteger sequence = new AtomicInteger();

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			generateCallCount.incrementAndGet();
			return "nonbatch-" + sequence.incrementAndGet();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	public static class CountingIdGenerator implements BeforeExecutionGenerator {
		static final AtomicInteger generateCallCount = new AtomicInteger();
		static final AtomicInteger generateBatchCallCount = new AtomicInteger();
		static final AtomicLong next = new AtomicLong( 1000 );

		public CountingIdGenerator(CountingId annotation, Member member, GeneratorCreationContext context) {
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			generateCallCount.incrementAndGet();
			return next.incrementAndGet();
		}

		@Override
		public boolean supportsBatchGeneration() {
			return true;
		}

		@Override
		public @Nonnull Object[] generateBatch(
				@Nonnull SharedSessionContractImplementor session,
				@Nonnull GenerationRequests requests,
				@Nonnull EventType eventType) {
			generateBatchCallCount.incrementAndGet();
			final Object[] ids = new Object[requests.size()];
			for ( int i = 0; i < ids.length; i++ ) {
				ids[i] = next.incrementAndGet();
			}
			return ids;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}
}
