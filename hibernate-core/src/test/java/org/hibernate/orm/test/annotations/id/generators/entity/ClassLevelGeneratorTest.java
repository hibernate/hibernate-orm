package org.hibernate.orm.test.annotations.id.generators.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{ClassLevelGeneratorTest.EntityWithAnonSequenceGenerator.class,
		ClassLevelGeneratorTest.EntityWithAnonTableGenerator.class})
@FailureExpected( reason = "Support for unnamed generators is not implemented yet" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18498" )
public class ClassLevelGeneratorTest {
	@Test
	void testAnonGenerator(SessionFactoryScope scope) {
		scope.inSession(s-> {
			EntityWithAnonSequenceGenerator entity1 = new EntityWithAnonSequenceGenerator();
			EntityWithAnonTableGenerator entity2 = new EntityWithAnonTableGenerator();
			s.persist(entity1);
			s.persist(entity2);
			assertEquals(42, entity1.id);
			assertEquals(69, entity2.id);
		});
	}
	@Entity
	@SequenceGenerator(initialValue = 42)
	static class EntityWithAnonSequenceGenerator {
		@Id
		@GeneratedValue
		long id;
	}
	@Entity
	@TableGenerator(initialValue = 69)
	static class EntityWithAnonTableGenerator {
		@Id
		@GeneratedValue
		long id;
	}
}
