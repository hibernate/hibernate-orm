package org.hibernate.orm.test.stateless;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = StatelessSessionStatisticsTest.Person.class)
@ServiceRegistry(settings = @Setting(name = GENERATE_STATISTICS, value = "true"))
public class StatelessSessionStatisticsTest {
	@Test
	void test(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertEquals(0, statistics.getEntityInsertCount());
		assertEquals(0, statistics.getEntityUpdateCount());
		assertEquals(0, statistics.getEntityDeleteCount());
		assertEquals(0, statistics.getEntityLoadCount());
		Person person = new Person();
		person.name = "Gavin";
		scope.inStatelessTransaction(s -> s.insert(person));
		assertEquals(1, statistics.getEntityInsertCount());
		scope.inStatelessSession(s -> s.get(Person.class, person.id));
		assertEquals(1, statistics.getEntityLoadCount());
		person.name = "Gavin King";
		scope.inStatelessTransaction(s -> s.update(person));
		assertEquals(1, statistics.getEntityUpdateCount());
		scope.inStatelessSession(s -> s.get(Person.class, person.id));
		assertEquals(2, statistics.getEntityLoadCount());
		scope.inStatelessTransaction(s -> s.delete(person));
		assertEquals(1, statistics.getEntityDeleteCount());
		assertEquals(3, statistics.getTransactionCount());
	}

	@Entity(name="Entity")
	static class Person {
		@Id @GeneratedValue
		long id;
		@Basic(optional = false)
		String name;
	}
}
