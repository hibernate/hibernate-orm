/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generator.batch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Nonnull;
import org.hibernate.annotations.IdGeneratorType;
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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		CascadeBatchGenerationTest.Parent.class,
		CascadeBatchGenerationTest.Child.class,
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20737")
class CascadeBatchGenerationTest {

	@BeforeEach
	void resetCounters() {
		SequenceBatchGenerator.generateCallCount.set( 0 );
		SequenceBatchGenerator.generateBatchCallCount.set( 0 );
		SequenceBatchGenerator.totalRequestCount.set( 0 );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testCascadedCollectionBatchGeneration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			for ( int i = 0; i < 5; i++ ) {
				parent.addChild( new Child() );
			}
			session.persist( parent );
			// Ensure the invariant, that the identifier is set after the #persist call succeeds
			assertThat( parent.getId() ).isNotNull();
			for ( Child child : parent.getChildren() ) {
				assertThat( child.getId() ).isNotNull();
			}
		} );

		assertThat( SequenceBatchGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be called for every #persist (1 parent)" )
				.isEqualTo( 1 );
		assertThat( SequenceBatchGenerator.totalRequestCount.get() )
				.as( "batch request should contain 5 items (for every Child)" )
				.isEqualTo( 5 );
		assertThat( SequenceBatchGenerator.generateCallCount.get() )
				.as( "generate() should not be called when batching" )
				.isEqualTo( 0 );

		scope.inTransaction( session -> {
			final var parents = session.createQuery(
					"from CascadeBatchParent p left join fetch p.children", Parent.class
			).getResultList();
			assertThat( parents ).hasSize( 1 );
			final Parent parent = parents.get( 0 );
			assertThat( parent.getChildren() ).hasSize( 5 );
		} );
	}

	@Test
	void testMultipleParentsWithCascadedChildren(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int p = 0; p < 3; p++ ) {
				final Parent parent = new Parent( (long) p + 1 );
				for ( int c = 0; c < 2; c++ ) {
					parent.addChild( new Child() );
				}
				session.persist( parent );
				// Ensure the invariant, that the identifier is set after the #persist call succeeds
				assertThat( parent.getId() ).isNotNull();
				for ( Child child : parent.getChildren() ) {
					assertThat( child.getId() ).isNotNull();
				}
			}
		} );

		assertThat( SequenceBatchGenerator.generateBatchCallCount.get() )
				.as( "generateBatch should be called for every #persist (3 parents)" )
				.isEqualTo( 3 );
		assertThat( SequenceBatchGenerator.totalRequestCount.get() )
				.as( "batch request should contain 6 items (for every Child)" )
				.isEqualTo( 6 );
		assertThat( SequenceBatchGenerator.generateCallCount.get() )
				.as( "generate() should not be called when batching" )
				.isEqualTo( 0 );
	}

	// --- Entities ---

	@Entity(name = "CascadeBatchParent")
	static class Parent {
		@Id
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}
	}

	@Entity(name = "CascadeBatchChild")
	static class Child {
		@Id
		@BatchSequenceId
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Parent parent;

		public Child() {
		}

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	// --- Annotation ---

	@IdGeneratorType(SequenceBatchGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	@interface BatchSequenceId {
	}

	// --- Generator ---

	public static class SequenceBatchGenerator implements BeforeExecutionGenerator {
		static final AtomicInteger generateCallCount = new AtomicInteger();
		static final AtomicInteger generateBatchCallCount = new AtomicInteger();
		static final AtomicInteger totalRequestCount = new AtomicInteger();

		private final AtomicLong sequence = new AtomicLong();

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			generateCallCount.incrementAndGet();
			return sequence.incrementAndGet();
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
				results[i] = sequence.incrementAndGet();
			}
			return results;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}
}
