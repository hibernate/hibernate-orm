/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				AutoFlushOnUpdateQueryTest.FruitLogEntry.class,
				AutoFlushOnUpdateQueryTest.Fruit.class,
		}
)
@SessionFactory
public class AutoFlushOnUpdateQueryTest {

	public static final String FRUIT_NAME = "Apple";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Fruit( FRUIT_NAME ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFlushIsExecuted(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Fruit fruit = session
							.createQuery(
									"select f from Fruit f where f.name = :name",
									Fruit.class
							).setParameter( "name", FRUIT_NAME ).getSingleResult();

					FruitLogEntry logEntry = new FruitLogEntry( fruit, "foo" );
					session.persist( logEntry );

					session.createMutationQuery( "update Fruit f set f.logEntry = :logEntry where f.id = :fruitId" )
							.setParameter( "logEntry", logEntry )
							.setParameter( "fruitId", fruit.getId() ).executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					Fruit fruit = session
							.createQuery(
									"select f from Fruit f where f.name = :name",
									Fruit.class
							).setParameter( "name", FRUIT_NAME ).getSingleResult();
					assertThat( fruit.getLogEntry() ).isNotNull();
				}
		);
	}

	@Test
	public void testFlushIsExecuted2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Fruit fruit = session
							.createQuery(
									"select f from Fruit f where f.name = :name",
									Fruit.class
							).setParameter( "name", FRUIT_NAME ).getSingleResult();

					FruitLogEntry logEntry = new FruitLogEntry( fruit, "foo" );
					session.persist( logEntry );

					session.createMutationQuery( "update Fruit f set f.logEntry.id = :logEntryId where f.id = :fruitId" )
							.setParameter( "logEntryId", logEntry.getId() )
							.setParameter( "fruitId", fruit.getId() ).executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					Fruit fruit = session
							.createQuery(
									"select f from Fruit f where f.name = :name",
									Fruit.class
							).setParameter( "name", FRUIT_NAME ).getSingleResult();
					assertThat( fruit.getLogEntry() ).isNotNull();
				}
		);
	}

	@Entity(name = "Fruit")
	public static class Fruit {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToOne
		private FruitLogEntry logEntry;

		public Fruit() {
		}

		public Fruit(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public FruitLogEntry getLogEntry() {
			return logEntry;
		}
	}

	@Entity(name = "FruitLogEntry")
	public static class FruitLogEntry {

		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(mappedBy = "logEntry")
		private Fruit fruit;

		private String logComments;

		public FruitLogEntry(Fruit fruit, String comment) {
			this.fruit = fruit;
			this.logComments = comment;
		}

		FruitLogEntry() {
		}

		public Long getId() {
			return id;
		}

		public Fruit getFruit() {
			return fruit;
		}

		public String getLogComments() {
			return logComments;
		}
	}
}
