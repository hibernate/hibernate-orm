package org.hibernate.orm.test.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

@JiraKey("HHH-18288")
@SessionFactory
@DomainModel(annotatedClasses = {SubclassIndexTest.Foo.class, SubclassIndexTest.Bar.class})
public class SubclassIndexTest {

	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory();
	}

	@Entity
	@Table(name = "FOO")
	static class Foo {
		@Id
		long id;
	}

	@Entity
	@Table(indexes = @Index(name="IDX", columnList = "text"))
	static class Bar extends Foo {
		@OneToMany
		@OrderColumn
		List<Foo> foo;

		String text;
	}
}
