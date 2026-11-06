package org.hibernate.orm.test.inheritance.discriminator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import org.hibernate.annotations.JavaType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;

@DomainModel(
		annotatedClasses = {
				GenericSpecializationInConsecutiveConcreteDescendantsTest.GenericParent.class,
				GenericSpecializationInConsecutiveConcreteDescendantsTest.Middle.class,
				GenericSpecializationInConsecutiveConcreteDescendantsTest.Child.class
		}
)
@SessionFactory
public class GenericSpecializationInConsecutiveConcreteDescendantsTest {
	@Test
	void specializationIsNotRepeatedForConcreteDescendant(SessionFactoryScope scope) {
		scope.getSessionFactory();
	}

	@Entity(name = "GenericParent")
	@Inheritance(strategy = SINGLE_TABLE)
	public abstract static class GenericParent<T> {
		@Id
		private String id;

		@JavaType(StringJavaType.class)
		private T property;
	}

	@Entity(name = "GenericMiddle")
	public static class Middle extends GenericParent<String> {
	}

	@Entity(name = "GenericChild")
	public static class Child extends Middle {
	}
}
