package org.hibernate.orm.test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@SessionFactory
@DomainModel(annotatedClasses = {EnumTest.A.class, EnumTest.B.class})
public class EnumTest {

	@Test public void test(SessionFactoryScope scope) {
		A a = new A();
		B b = new B();
		b.cost = new BigDecimal(1.0);
		b.type = Enum.B;
		b.code = "123";
		a.b = b;
		scope.inTransaction(s -> s.persist(a));
	}

	public enum Enum { A, B }

	@Entity
	public static class B {
		@Id
		@Column(length = 12)
		String code;

		@Id
		@Column(precision = 10, scale = 0)
		BigDecimal cost;

		@Id
		@Enumerated(EnumType.STRING)
		Enum type;

		@Id
		long id;
	}

	@Entity(name = "A")
	public static class A
	{
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "id")
		private int id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		B b;
	}
}
