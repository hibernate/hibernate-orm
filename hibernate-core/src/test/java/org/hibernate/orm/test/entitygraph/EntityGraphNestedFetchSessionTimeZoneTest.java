package org.hibernate.orm.test.entitygraph;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JDBC time zone is set on the session only, not on the session factory.
 */
@DomainModel(annotatedClasses = EntityGraphNestedFetchSessionTimeZoneTest.Owner.class)
@SessionFactory
class EntityGraphNestedFetchSessionTimeZoneTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }
	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void timestampUsesSessionJdbcTimeZone(FetchMethod method, SessionFactoryScope scope) {
		var expected = new Timestamp(1704112496789L);
		var timeZone = TimeZone.getTimeZone("Pacific/Honolulu");
		try (var s = scope.getSessionFactory().withOptions().jdbcTimeZone(timeZone).openSession()) {
			s.getTransaction().begin();
			Owner owner = new Owner(); owner.id = 1L; owner.times.add(expected); s.persist(owner);
			s.getTransaction().commit();
		}
		try (var s = scope.getSessionFactory().withOptions().jdbcTimeZone(timeZone).openSession()) {
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("times").addOption(method);
			assertThat(s.find(graph, 1L).times).containsExactly(expected);
		}
	}
	@Entity(name = "STZOwner") static class Owner {
		@Id Long id;
		@ElementCollection List<Timestamp> times = new ArrayList<>();
	}
}
