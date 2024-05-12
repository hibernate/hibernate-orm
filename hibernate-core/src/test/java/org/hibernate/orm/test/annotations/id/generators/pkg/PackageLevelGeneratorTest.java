package org.hibernate.orm.test.annotations.id.generators.pkg;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{PackageLevelGeneratorTest.EntityWithAnonSequenceGenerator.class,
		PackageLevelGeneratorTest.EntityWithAnonTableGenerator.class})
public class PackageLevelGeneratorTest {
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
	static class EntityWithAnonSequenceGenerator {
		@Id
		@GeneratedValue
		long id;
	}
	@Entity
	static class EntityWithAnonTableGenerator {
		@Id
		@GeneratedValue
		long id;
	}
}
