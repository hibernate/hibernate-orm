/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				ImplicitInstantiationTest.Thing.class
		}
)
@SessionFactory
public class ImplicitInstantiationTest {

	static class Record {
		Long id;
		String name;
		public Record(Long id, String name) {
			this.id = id;
			this.name = name;
		}
		Long id() {
			return id;
		}
		String name() {
			return name;
		}
	}

	@Test
	public void testRecordInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Record result = session.createSelectionQuery("select id, upper(name) from Thing", Record.class).getSingleResult();
					assertEquals( 1L, result.id() );
					assertEquals( "THING", result.name() );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testSqlRecordInstantiationWithoutMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Record result = session.createNativeQuery( "select id, upper(name) as name from thingy_table", Record.class)
							.addSynchronizedEntityClass(Thing.class)
							.getSingleResult();
					assertEquals( 1L, result.id() );
					assertEquals( "THING", result.name() );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testSqlRecordInstantiationWithMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Record result = session.createNativeQuery( "select id, upper(name) as name from thingy_table", Record.class)
							.addScalar("id", Long.class)
							.addScalar("name", String.class)
							.addSynchronizedEntityClass(Thing.class)
							.getSingleResult();
					assertEquals( 1L, result.id() );
					assertEquals( "THING", result.name() );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testTupleInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Tuple result = session.createQuery("select id as id, upper(name) as name from Thing", Tuple.class).getSingleResult();
					assertEquals( 1L, result.get("id") );
					assertEquals( "THING", result.get("name") );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testTupleInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Tuple result = session.createSelectionQuery("select id, upper(name) from Thing", Tuple.class).getSingleResult();
					assertEquals( 1L, result.get(0) );
					assertEquals( "THING", result.get(1) );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testMapInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Map<?,?> result = session.createSelectionQuery("select id, upper(name) from Thing", Map.class).getSingleResult();
					assertEquals( 1L, result.get("0") );
					assertEquals( "THING", result.get("1") );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testMapInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Map<?,?> result = session.createQuery("select id as id, upper(name) as name from Thing", Map.class).getSingleResult();
					assertEquals( 1L, result.get("id") );
					assertEquals( "THING", result.get("name") );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testListInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					List<?> result = session.createSelectionQuery("select id, upper(name) from Thing", List.class).getSingleResult();
					assertEquals( 1L, result.get(0) );
					assertEquals( "THING", result.get(1) );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testListInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					List<?> result = session.createQuery("select id as id, upper(name) as name from Thing", List.class).getSingleResult();
					assertEquals( 1L, result.get(0) );
					assertEquals( "THING", result.get(1) );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testSqlTupleInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Tuple result = session.createNativeQuery( "select id as id, upper(name) as name from thingy_table", Tuple.class)
							.addSynchronizedEntityClass(Thing.class)
							.getSingleResult();
					assertEquals( 1L, result.get("id") );
					assertEquals( "THING", result.get("name") );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testSqlMapInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					Map<?,?> result = session.createNativeQuery( "select id as id, upper(name) as name from thingy_table", Map.class)
							.addSynchronizedEntityClass(Thing.class)
							.getSingleResult();
					assertEquals( 1L, result.get("id") );
					assertEquals( "THING", result.get("name") );
					session.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testSqlListInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(new Thing(1L, "thing"));
					List<?> result = session.createNativeQuery( "select id, upper(name) as name from thingy_table", List.class)
							.addSynchronizedEntityClass(Thing.class)
							.getSingleResult();
					assertEquals( 1L, result.get(0) );
					assertEquals( "THING", result.get(1) );
					session.getTransaction().setRollbackOnly();
				}
		);
	}


	@Entity(name = "Thing")
	@Table(name = "thingy_table")
	public class Thing {
		private Long id;

		private String name;

		public Thing(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		Thing() {
		}

		@Id
		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
