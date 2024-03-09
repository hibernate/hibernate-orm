package org.hibernate.orm.test.mapping.attributebinder.typebinder;

import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = ResultCheckBinderTest.Entity.class)
public class ResultCheckBinderTest {
	@Test void test(SessionFactoryScope scope) {
		Entity entity = new Entity();
		scope.inStatelessTransaction(s -> s.insert(entity) );
		scope.inStatelessTransaction(s -> s.delete(entity) );
		scope.inStatelessTransaction( s -> s.update(entity) );
		scope.inStatelessTransaction(s -> s.delete(entity) );
	}
	@NoResultCheck
	@jakarta.persistence.Entity
	static class Entity {
		@Id long id;
	}
}
