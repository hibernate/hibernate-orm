package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * On MySQL and MariaDB the aggregate is built with {@code group_concat}, which silently truncates
 * its result to {@code group_concat_max_len}. With the limit set to the length of exactly one row,
 * {@code [11],[22]} is truncated to {@code [11]}, still valid JSON once wrapped as {@code [[11]]}.
 */
@RequiresDialect(MySQLDialect.class)
@DomainModel(annotatedClasses = EntityGraphNestedFetchGroupConcatTest.Owner.class)
@SessionFactory
class EntityGraphNestedFetchGroupConcatTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }

	@Test
	void byIdIsNotTruncated(SessionFactoryScope scope) {
		persistOwner( scope );
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class);
			graph.addAttributeNode("numbers").addOption(FetchMethod.BY_ID);
			withOneRowGroupConcatMaxLen(s, () -> assertThat(s.find(graph, 1L).numbers)
					.containsExactlyInAnyOrder(11, 22)
			);
		});
	}

	@Test
	void nestedFailsWhenTruncated(SessionFactoryScope scope) {
		persistOwner( scope );
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class);
			graph.addAttributeNode("numbers").addOption(FetchMethod.NESTED);
			withOneRowGroupConcatMaxLen(s, () -> assertThatThrownBy(() -> s.find(graph, 1L))
					.isInstanceOf(HibernateException.class)
					.hasMessageContaining("1 of 2 rows were read")
			);
		});
	}

	// "[11]" is 4 bytes, the minimum MySQL accepts
	private static void withOneRowGroupConcatMaxLen(Session s, Runnable action) {
		s.doWork(c -> c.createStatement().execute("set session group_concat_max_len = 4"));
		try {
			action.run();
		}
		finally {
			s.doWork(c -> c.createStatement().execute("set session group_concat_max_len = default"));
		}
	}

	private static void persistOwner(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; owner.numbers.add(11); owner.numbers.add(22); s.persist(owner);
		});
	}

	@Entity(name = "GCOwner") static class Owner {
		@Id Long id;
		@ElementCollection List<Integer> numbers = new ArrayList<>();
	}
}
