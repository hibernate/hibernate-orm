/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.id.generators.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{FieldLevelGeneratorTest.EntityWithAnonSequenceGenerator.class,
		FieldLevelGeneratorTest.EntityWithAnonTableGenerator.class})
public class FieldLevelGeneratorTest {
	@Test
	void testAnonGenerator(SessionFactoryScope scope) {
		scope.inSession(s-> {
			EntityWithAnonSequenceGenerator entity1 = new EntityWithAnonSequenceGenerator();
			EntityWithAnonTableGenerator entity2 = new EntityWithAnonTableGenerator();
			s.persist(entity1);
			s.persist(entity2);
			assertEquals(42, entity1.id);
			assertEquals(70, entity2.id);
		});
	}

	@Entity(name = "EntityWithAnonSequenceGenerator")
	static class EntityWithAnonSequenceGenerator {
		@Id
		@GeneratedValue
		@SequenceGenerator(initialValue = 42)
		long id;
	}

	@Entity(name = "EntityWithAnonTableGenerator")
	static class EntityWithAnonTableGenerator {
		@Id
		@GeneratedValue
		@TableGenerator(initialValue = 69)
		long id;
	}
}
