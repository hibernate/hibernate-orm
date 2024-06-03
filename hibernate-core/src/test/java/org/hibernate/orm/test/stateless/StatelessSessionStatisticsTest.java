package org.hibernate.orm.test.stateless;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.hibernate.graph.GraphSemantic.FETCH;
import static org.hibernate.graph.GraphSemantic.LOAD;
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
		person.handles.add("@1ovthafew");
		scope.inStatelessTransaction(s -> s.insert(person));
		assertEquals(1, statistics.getEntityInsertCount());
		assertEquals(1, statistics.getCollectionRecreateCount());
		scope.inStatelessSession(s -> s.get(Person.class, person.id));
		assertEquals(1, statistics.getEntityLoadCount());
		assertEquals(0, statistics.getEntityFetchCount());
		assertEquals(1, statistics.getCollectionLoadCount());
		assertEquals(0, statistics.getCollectionFetchCount());
		person.name = "Gavin King";
		scope.inStatelessTransaction(s -> s.update(person));
		assertEquals(1, statistics.getEntityUpdateCount());
		assertEquals(1, statistics.getCollectionUpdateCount());
		scope.inStatelessSession(s -> s.get(s.createEntityGraph(Person.class), LOAD, person.id));
		assertEquals(2, statistics.getEntityLoadCount());
		assertEquals(2, statistics.getCollectionLoadCount());
		assertEquals(0, statistics.getCollectionFetchCount());
		scope.inStatelessSession(s -> s.get(s.createEntityGraph(Person.class), FETCH, person.id));
		assertEquals(3, statistics.getEntityLoadCount());
		assertEquals(2, statistics.getCollectionLoadCount());
		assertEquals(0, statistics.getCollectionFetchCount());
		scope.inStatelessSession(s -> s.fetch(s.get(s.createEntityGraph(Person.class), FETCH, person.id).handles));
		assertEquals(4, statistics.getEntityLoadCount());
		assertEquals(3, statistics.getCollectionLoadCount());
		assertEquals(1, statistics.getCollectionFetchCount());
		scope.inStatelessSession(s -> s.createQuery("from Person", Person.class).getSingleResult());
		assertEquals(5, statistics.getEntityLoadCount());
		assertEquals(4, statistics.getCollectionLoadCount());
		assertEquals(2, statistics.getCollectionFetchCount());
		person.handles.add("hello world");
		scope.inStatelessTransaction(s -> s.upsert(person));
		assertEquals(2, statistics.getCollectionUpdateCount());
		scope.inStatelessTransaction(s -> s.delete(person));
		assertEquals(1, statistics.getEntityDeleteCount());
		assertEquals(1, statistics.getCollectionRemoveCount());
		assertEquals(4, statistics.getTransactionCount());
	}

	@Entity(name="Person")
	static class Person {
		@Id @GeneratedValue
		long id;
		@Basic(optional = false)
		String name;
		@ElementCollection(fetch = FetchType.EAGER)
		List<String> handles = new ArrayList<>();
	}
}
