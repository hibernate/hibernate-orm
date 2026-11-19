package org.hibernate.orm.test.generator.batch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GenerationRequests;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that batch generation of a generated embeddable attribute passes
 * the owning entity to the nested generator, just like single-value generation does.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		EmbeddableBatchGenerationOwnerTest.OwnerEntity.class,
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20737")
class EmbeddableBatchGenerationOwnerTest {

	@BeforeEach
	void resetCounters() {
		OwnerNameGenerator.generateCallCount.set( 0 );
		OwnerNameGenerator.generateBatchCallCount.set( 0 );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testNestedGeneratorReceivesOwningEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 3; i++ ) {
				session.persist( new OwnerEntity( (long) i, "owner-" + i ) );
			}
		} );

		assertThat( OwnerNameGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be used for the embeddable attribute" )
				.isEqualTo( 1 );
		assertThat( OwnerNameGenerator.generateCallCount.get() )
				.as( "generate() should not be called when batching" )
				.isEqualTo( 0 );

		scope.inTransaction( session -> {
			for ( long i = 1; i <= 3; i++ ) {
				final OwnerEntity entity = session.find( OwnerEntity.class, i );
				assertThat( entity.getInfo() ).isNotNull();
				assertThat( entity.getInfo().getCode() ).isEqualTo( "code-owner-" + i );
			}
		} );
	}

	@Entity(name = "EmbeddableBatchOwner")
	static class OwnerEntity {
		@Id
		private Long id;
		private String name;
		@Embedded
		private Info info = new Info();

		public OwnerEntity() {
		}

		public OwnerEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Info getInfo() {
			return info;
		}
	}

	@Embeddable
	static class Info {
		@OwnerNameGenerated
		private String code;
		private String label;

		public String getCode() {
			return code;
		}

		public String getLabel() {
			return label;
		}
	}

	@ValueGenerationType(generatedBy = OwnerNameGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface OwnerNameGenerated {
	}

	/**
	 * A generator which reads a property from the owning entity.
	 */
	public static class OwnerNameGenerator implements BeforeExecutionGenerator {
		static final AtomicInteger generateCallCount = new AtomicInteger();
		static final AtomicInteger generateBatchCallCount = new AtomicInteger();

		private static OwnerEntity owner(Object owner) {
			if ( owner instanceof OwnerEntity ownerEntity ) {
				return ownerEntity;
			}
			throw new AssertionError( "Expected the owning entity but got: " + owner );
		}

		@Override
		public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
			owner( entity );
			return true;
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			generateCallCount.incrementAndGet();
			return "code-" + owner( owner ).getName();
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
			final Object[] results = new Object[requests.size()];
			for ( int i = 0; i < requests.size(); i++ ) {
				results[i] = "code-" + owner( requests.get( i ).owner() ).getName();
			}
			return results;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}
}
