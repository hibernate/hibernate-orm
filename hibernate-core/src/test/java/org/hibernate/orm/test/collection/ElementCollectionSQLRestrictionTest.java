/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ElementCollectionSQLRestrictionTest.TaskEntity.class,
		ElementCollectionSQLRestrictionTest.LocalizedLabel.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class ElementCollectionSQLRestrictionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TaskEntity task1 = new TaskEntity( 1 );
			task1.getNames().add( new LocalizedLabel( "task:name", "test-name" ) );
			task1.getTypes().add( new LocalizedLabel( "task:type", "test-type" ) );
			session.persist( task1 );
			final TaskEntity task2 = new TaskEntity( 2 );
			task2.getNames().addAll( Set.of(
					new LocalizedLabel( "task:name", "test-name1" ),
					new LocalizedLabel( "task:name", "test-name2" )
			) );
			task2.getTypes().addAll( Set.of(
					new LocalizedLabel( "task:type", "test-type1" ),
					new LocalizedLabel( "task:type", "test-type2" )
			) );
			session.persist( task2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TaskEntity" ).executeUpdate() );
	}

	@Test
	public void testRemoveEmptyCollection(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 1 );
			assertThat( task.getNames() ).hasSize( 1 );
			assertThat( task.getTypes() ).hasSize( 1 );
			task.getNames().clear();
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 3 );
		inspector.getSqlQueries().forEach( query -> assertThat( query ).containsAnyOf( "task:", "insert" ) );
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 1 );
			assertThat( task.getNames() ).hasSize( 0 );
			assertThat( task.getTypes() ).hasSize( 1 );
		} );
	}

	@Test
	public void testRemoveNonEmptyCollection(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 2 );
			assertThat( task.getNames() ).hasSize( 2 );
			assertThat( task.getTypes() ).hasSize( 2 );
			task.getTypes().remove( task.getTypes().iterator().next() );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 5 );
		inspector.getSqlQueries().forEach( query -> assertThat( query ).containsAnyOf( "task:", "insert" ) );
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 2 );
			assertThat( task.getNames() ).hasSize( 2 );
			assertThat( task.getTypes() ).hasSize( 1 );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 2 );
			task.getNames().forEach( n -> n.setLabel( n.getLabel().replace( "test", "updated" ) ) );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 3 );
		inspector.getSqlQueries().forEach( query -> assertThat( query ).containsAnyOf( "task:", "insert" ) );
		scope.inTransaction( session -> {
			final TaskEntity task = session.find( TaskEntity.class, 2 );
			task.getNames().forEach( n -> assertThat( n.getLabel() ).startsWith( "updated" ) );
			task.getTypes().forEach( t -> assertThat( t.getLabel() ).startsWith( "test" ) );
		} );
	}

	@Entity( name = "TaskEntity" )
	public static class TaskEntity {
		@Id
		private Integer id;

		@ElementCollection
		@CollectionTable( name = "t_localized_label" )
		@SQLRestriction( "identifier ='task:name'" )
		private Set<LocalizedLabel> names = new HashSet<>();

		@ElementCollection
		@CollectionTable( name = "t_localized_label" )
		@SQLRestriction( "identifier ='task:type'" )
		private Set<LocalizedLabel> types = new HashSet<>();

		public TaskEntity() {
		}

		public TaskEntity(Integer id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public Set<LocalizedLabel> getNames() {
			return names;
		}

		public Set<LocalizedLabel> getTypes() {
			return types;
		}
	}

	@Embeddable
	public static class LocalizedLabel {
		private String identifier;

		private String label;

		public LocalizedLabel() {
		}

		public LocalizedLabel(String identifier, String label) {
			this.identifier = identifier;
			this.label = label;
		}

		public String getIdentifier() {
			return identifier;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}
}
