package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.LocalTime;
import jakarta.persistence.*;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

/// Oracle predicates and projections must use the same LocalTime encoding as persistence.
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@DomainModel(annotatedClasses=OracleLocalTimeLiteralOrmTests.Event.class)
@SessionFactory(useCollectingStatementInspector=true)
class OracleLocalTimeLiteralOrmTests {
	@AfterEach void clean(SessionFactoryScope scope) {scope.dropData();}

	@ParameterizedTest @ValueSource(strings={"00:00:00", "12:34:56", "12:34:56.123456", "23:59:59.999999"})
	void literalMatchesBoundValue(String text, SessionFactoryScope scope) {
		var value=LocalTime.parse(text);
		scope.inTransaction(s->{TemporalMatrixEnvironment.prepareSession(s,true);var e=new Event();e.id=1;e.clock=value;s.persist(e);});
		scope.inTransaction(s->{
			TemporalMatrixEnvironment.prepareSession(s,false);
			assertThat(s.find(Event.class,1).clock).isEqualTo(value);
			assertThat(s.createSelectionQuery("from OracleTimeLiteralEvent e where e.clock=:p",Event.class)
				.setParameter("p",value).getResultList()).hasSize(1);
			var cb=s.getCriteriaBuilder();var q=cb.createQuery(Event.class);var r=q.from(Event.class);
			q.select(r).where(cb.equal(r.get("clock"),cb.literal(value)));
			scope.getCollectingStatementInspector().clear();
			assertThat(s.createQuery(q).getResultList()).hasSize(1);
			assertThat(scope.getCollectingStatementInspector().getSqlQueries()).singleElement()
				.asString().contains("1970-01-01").doesNotContain("?");
			var projection=cb.createQuery(LocalTime.class);
			projection.select(cb.literal(value));
			assertThat(s.createQuery(projection).getSingleResult()).isEqualTo(value);
		});
	}
	@Entity(name="OracleTimeLiteralEvent") static class Event {
		@Id int id;
		@Column(secondPrecision=6) LocalTime clock;
	}
}
