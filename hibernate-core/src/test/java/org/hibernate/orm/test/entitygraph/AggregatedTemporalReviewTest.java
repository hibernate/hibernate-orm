package org.hibernate.orm.test.entitygraph;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.FetchMethod;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = AggregatedTemporalReviewTest.Owner.class)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JDBC_TIME_ZONE, value = "Pacific/Honolulu"))
class AggregatedTemporalReviewTest {
	@AfterEach void clean(SessionFactoryScope scope) { scope.getSessionFactory().getSchemaManager().truncate(); }
	@ParameterizedTest
	@EnumSource(value = FetchMethod.class, names = {"BY_ID", "NESTED"})
	void timestampUsesJdbcTimeZone(FetchMethod method, SessionFactoryScope scope) {
		var expected = new Timestamp(1704112496789L);
		scope.inTransaction(s -> {
			Owner owner = new Owner(); owner.id = 1L; owner.times.add(expected); s.persist(owner);
		});
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Owner.class); graph.addAttributeNode("times").addOption(method);
			assertThat(s.find(graph, 1L).times).containsExactly(expected);
		});
	}
	@Entity(name = "ATOwner") static class Owner {
		@Id Long id;
		@ElementCollection List<Timestamp> times = new ArrayList<>();
	}
}
