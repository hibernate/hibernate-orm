/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.internal.NativeQueryImpl;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaDelete;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiresDialect(H2Dialect.class)
@Jpa(annotatedClasses = {TupleNativeQueryTest.User.class})
public class TupleNativeQueryTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			User user = new User("Arnold");
			entityManager.persist(user);
		});
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaDelete<User> delete = entityManager.getCriteriaBuilder().createCriteriaDelete(User.class);
			delete.from(User.class);
			entityManager.createQuery(delete).executeUpdate();
		});
	}

	@Test
	public void testPositionalGetterShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get(0));
			assertEquals("Arnold", tuple.get(1));
		});
	}

	@Test
	public void testPositionalGetterWithClassShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get(0, Long.class));
			assertEquals("Arnold", tuple.get(1, String.class));
		});
	}

	@Test
	public void testPositionalGetterShouldThrowExceptionWhenLessThanZeroGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(-1);
					}
			);
		});
	}

	@Test
	public void testPositionalGetterShouldThrowExceptionWhenTupleSizePositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(2);
					}
			);
		});
	}

	@Test
	public void testPositionalGetterShouldThrowExceptionWhenExceedingPositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(3);
					}
			);
		});
	}

	@Test
	public void testAliasGetterWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ID"));
			assertEquals("Arnold", tuple.get("FIRSTNAME"));
		});
	}

	@Test
	public void testAliasGetterShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleResult(entityManager);
			Tuple tuple = result.get(0);
			tuple.get("id");
		});
	}

	@Test
	public void testAliasGetterShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get("e");
					}
			);
		});
	}

	@Test
	public void testAliasGetterWithClassWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ID", Long.class));
			assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
		});
	}

	@Test
	public void testAliasGetterWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleAliasedResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ALIAS1"));
			assertEquals("Arnold", tuple.get("ALIAS2"));
		});
	}

	@Test
	public void testAliasGetterWithClassWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleAliasedResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ALIAS1", Long.class));
			assertEquals("Arnold", tuple.get("ALIAS2", String.class));
		});
	}

	@Test
	public void testToArrayShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getTupleResult(entityManager);
			Object[] result = tuples.get(0).toArray();
			assertArrayEquals(new Object[]{1L, "Arnold"}, result);
		});
	}

	@Test
	public void testGetElementsShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getTupleResult(entityManager);
			List<TupleElement<?>> result = tuples.get(0).getElements();
			assertEquals(2, result.size());
			assertEquals(Long.class, result.get(0).getJavaType());
			assertEquals("ID", result.get(0).getAlias());
			assertEquals(String.class, result.get(1).getJavaType());
			assertEquals("FIRSTNAME", result.get(1).getAlias());
		});
	}

	@Test
	public void testPositionalGetterWithNamedNativeQueryShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get(0));
			assertEquals("Arnold", tuple.get(1));
		});
	}

	@Test
	public void testPositionalGetterWithNamedNativeQueryWithClassShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get(0, Long.class));
			assertEquals("Arnold", tuple.get(1, String.class));
		});
	}

	@Test
	public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenLessThanZeroGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleNamedResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(-1);
					}
			);
		});
	}

	@Test
	public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenTupleSizePositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleNamedResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(2);
					}
			);
		});
	}

	@Test
	public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenExceedingPositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleNamedResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(3);
					}
			);
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ID"));
			assertEquals("Arnold", tuple.get("FIRSTNAME"));
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			tuple.get("id");
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getTupleNamedResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get("e");
					}
			);
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryWithClassWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ID", Long.class));
			assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard_with_alias");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ALIAS1"));
			assertEquals("Arnold", tuple.get("ALIAS2"));
		});
	}

	@Test
	public void testAliasGetterWithNamedNativeQueryWithClassWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleNamedResult(entityManager, "standard_with_alias");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ALIAS1", Long.class));
			assertEquals("Arnold", tuple.get("ALIAS2", String.class));
		});
	}

	@Test
	public void testToArrayShouldWithNamedNativeQueryWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getTupleNamedResult(entityManager, "standard");
			Object[] result = tuples.get(0).toArray();
			assertArrayEquals(new Object[]{1L, "Arnold"}, result);
		});
	}

	@Test
	public void testGetElementsWithNamedNativeQueryShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getTupleNamedResult(entityManager, "standard");
			List<TupleElement<?>> result = tuples.get(0).getElements();
			assertEquals(2, result.size());
			assertEquals(Long.class, result.get(0).getJavaType());
			assertEquals("ID", result.get(0).getAlias());
			assertEquals(String.class, result.get(1).getJavaType());
			assertEquals("FIRSTNAME", result.get(1).getAlias());
		});
	}

	@Test
	public void testStreamedPositionalGetterShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get(0));
			assertEquals("Arnold", tuple.get(1));
		});
	}

	@Test
	public void testStreamedPositionalGetterWithClassShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get(0, Long.class));
			assertEquals("Arnold", tuple.get(1, String.class));
		});
	}

	@Test
	public void testStreamedPositionalGetterShouldThrowExceptionWhenLessThanZeroGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(-1);
					}
			);
		});
	}

	@Test
	public void testStreamedPositionalGetterShouldThrowExceptionWhenTupleSizePositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(2);
					}
			);
		});
	}

	@Test
	public void testStreamedPositionalGetterShouldThrowExceptionWhenExceedingPositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get(3);
					}
			);
		});
	}

	@Test
	public void testStreamedAliasGetterWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ID"));
			assertEquals("Arnold", tuple.get("FIRSTNAME"));
		});
	}

	@Test
	public void testStreamedAliasGetterShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedTupleResult(entityManager);
			Tuple tuple = result.get(0);
			tuple.get("id");
		});
	}

	@Test
	public void testStreamedAliasGetterShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedTupleResult(entityManager);
						Tuple tuple = result.get(0);
						tuple.get("e");
					}
			);
		});
	}

	@Test
	public void testStreamedAliasGetterWithClassWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedTupleResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ID", Long.class));
			assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
		});
	}

	@Test
	public void testStreamedAliasGetterWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleAliasedResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ALIAS1"));
			assertEquals("Arnold", tuple.get("ALIAS2"));
		});
	}

	@Test
	public void testStreamedAliasGetterWithClassWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getTupleAliasedResult(entityManager);
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ALIAS1", Long.class));
			assertEquals("Arnold", tuple.get("ALIAS2", String.class));
		});
	}

	@Test
	public void testStreamedToArrayShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getStreamedTupleResult(entityManager);
			Object[] result = tuples.get(0).toArray();
			assertArrayEquals(new Object[]{1L, "Arnold"}, result);
		});
	}

	@Test
	public void testStreamedGetElementsShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getStreamedTupleResult(entityManager);
			List<TupleElement<?>> result = tuples.get(0).getElements();
			assertEquals(2, result.size());
			assertEquals(Long.class, result.get(0).getJavaType());
			assertEquals("ID", result.get(0).getAlias());
			assertEquals(String.class, result.get(1).getJavaType());
			assertEquals("FIRSTNAME", result.get(1).getAlias());
		});
	}

	@Test
	public void testStreamedPositionalGetterWithNamedNativeQueryShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get(0));
			assertEquals("Arnold", tuple.get(1));
		});
	}

	@Test
	public void testStreamedPositionalGetterWithNamedNativeQueryWithClassShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get(0, Long.class));
			assertEquals("Arnold", tuple.get(1, String.class));
		});
	}

	@Test
	public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenLessThanZeroGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(-1);
					}
			);
		});
	}

	@Test
	public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenTupleSizePositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(2);
					}
			);
		});
	}

	@Test
	public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenExceedingPositionGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get(3);
					}
			);
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ID"));
			assertEquals("Arnold", tuple.get("FIRSTNAME"));
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			tuple.get("id");
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
						Tuple tuple = result.get(0);
						tuple.get("e");
					}
			);
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryWithClassWithoutExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ID", Long.class));
			assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard_with_alias");
			Tuple tuple = result.get(0);
			assertEquals(1L, tuple.get("ALIAS1"));
			assertEquals("Arnold", tuple.get("ALIAS2"));
		});
	}

	@Test
	public void testStreamedAliasGetterWithNamedNativeQueryWithClassWithExplicitAliasShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard_with_alias");
			Tuple tuple = result.get(0);
			assertEquals(Long.valueOf(1L), tuple.get("ALIAS1", Long.class));
			assertEquals("Arnold", tuple.get("ALIAS2", String.class));
		});
	}

	@Test
	public void testStreamedToArrayShouldWithNamedNativeQueryWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getStreamedNamedTupleResult(entityManager, "standard");
			Object[] result = tuples.get(0).toArray();
			assertArrayEquals(new Object[]{1L, "Arnold"}, result);
		});
	}

	@Test
	public void testStreamedGetElementsWithNamedNativeQueryShouldWorkProperly(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getStreamedNamedTupleResult(entityManager, "standard");
			List<TupleElement<?>> result = tuples.get(0).getElements();
			assertEquals(2, result.size());
			assertEquals(Long.class, result.get(0).getJavaType());
			assertEquals("ID", result.get(0).getAlias());
			assertEquals(String.class, result.get(1).getJavaType());
			assertEquals("FIRSTNAME", result.get(1).getAlias());
		});
	}

	@Test
	@JiraKey(value = "HHH-11897")
	public void testGetElementsShouldNotThrowExceptionWhenResultContainsNullValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			User user = entityManager.find(User.class, 1L);
			user.firstName = null;
		});
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = getTupleResult(entityManager);
			final Tuple tuple = tuples.get(0);
			List<TupleElement<?>> result = tuple.getElements();
			assertEquals(2, result.size());
			final TupleElement<?> firstTupleElement = result.get(0);
			assertEquals(Long.class, firstTupleElement.getJavaType());
			assertEquals("ID", firstTupleElement.getAlias());
			assertEquals(1L, tuple.get(firstTupleElement.getAlias()));
			final TupleElement<?> secondTupleElement = result.get(1);
			assertEquals(Object.class, secondTupleElement.getJavaType());
			assertEquals("FIRSTNAME", secondTupleElement.getAlias());
			assertNull(tuple.get(secondTupleElement.getAlias()));
		});
	}

	@SuppressWarnings("unchecked")
	private List<Tuple> getTupleAliasedResult(EntityManager entityManager) {
		Query query = entityManager.createNativeQuery("SELECT id AS alias1, firstname AS alias2 FROM users", Tuple.class);
		return (List<Tuple>) query.getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<Tuple> getStreamedTupleAliasedResult(EntityManager entityManager) {
		NativeQueryImpl query = (NativeQueryImpl) entityManager.createNativeQuery(
				"SELECT id AS alias1, firstname AS alias2 FROM users",
				Tuple.class
		);
		return (List<Tuple>) query.stream().collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<Tuple> getTupleResult(EntityManager entityManager) {
		Query query = entityManager.createNativeQuery("SELECT id, firstname FROM users", Tuple.class);
		return (List<Tuple>) query.getResultList();
	}

	private List<Tuple> getTupleNamedResult(EntityManager entityManager, String name) {
		return entityManager.createNamedQuery(name, Tuple.class).getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<Tuple> getStreamedTupleResult(EntityManager entityManager) {
		NativeQuery query = (NativeQuery) entityManager.createNativeQuery( "SELECT id, firstname FROM users", Tuple.class);
		return (List<Tuple>) query.stream().collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<Tuple> getStreamedNamedTupleResult(EntityManager entityManager, String name) {
		return (List<Tuple>)((NativeQuery) entityManager.createNamedQuery(name, Tuple.class)).stream().collect(Collectors.toList());
	}

	@Entity
	@Table(name = "users")
	@NamedNativeQueries({
			@NamedNativeQuery(
					name = "standard",
					query = "SELECT id, firstname FROM users"
			),
			@NamedNativeQuery(
					name = "standard_with_alias",
					query = "SELECT id AS alias1, firstname AS alias2 FROM users"
			)
	})

	public static class User {
		@Id
		private long id;

		private String firstName;

		public User() {
		}

		public User(String firstName) {
			this.id = 1L;
			this.firstName = firstName;
		}
	}
}
