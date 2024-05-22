package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = NativeGeneratorTest.NativeEntity.class)
public class NativeGeneratorTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(s -> s.persist(new NativeEntity()));
	}
	@Entity
	public static class NativeEntity {
		@Id @NativeId
		long id;
		String data;
	}
}
