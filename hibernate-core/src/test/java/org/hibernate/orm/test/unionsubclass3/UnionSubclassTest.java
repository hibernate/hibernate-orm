/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass3;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author pholvs
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		UnionSubclassTest.Child.class,
		UnionSubclassTest.Parent.class,
		UnionSubclassTest.Father.class,
		UnionSubclassTest.Mother.class
})
@SessionFactory
public class UnionSubclassTest {

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey("HHH-12114")
	public void testUnionSubclassClassResults(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Father father = new Father();
			father.id = 1L;
			father.fathersDay = "FD1";

			Child child1 = new Child();
			child1.id = 2L;
			child1.parent = father;

			Mother mother1 = new Mother();
			mother1.id = 3L;
			mother1.mothersDay = "MD1";

			Child child2 = new Child();
			child2.id = 4L;
			child2.parent = mother1;

			Mother mother2 = new Mother();
			mother2.id = 5L;
			mother2.mothersDay = "MD2";

			Child child3 = new Child();
			child3.id = 6L;
			child3.parent = mother2;

			Mother mother4 = new Mother();
			mother4.id = 7L;
			mother4.mothersDay = "MD3";

			Child child4 = new Child();
			child4.id = 8L;
			child4.parent = mother4;

			session.persist(father);
			session.persist(mother1);
			session.persist(mother2);
			session.persist(mother4);

			session.persist(child1);
			session.persist(child2);
			session.persist(child3);
			session.persist(child4);
		});

		factoryScope.inTransaction( session -> {
			var results = session.createQuery(
				"select c " +
				"from Child c"
				+ " left join c.parent p1 "
				+ " left join c.parent p2 "
				+ " where " +
				"		(TYPE(p1) = Father and p1.fathersDay = 'FD1') " +
				"	or "
				+ "  	(TYPE(p2) = Mother and p2.mothersDay = 'MD1')")
			.getResultList();
			Assertions.assertEquals( 2, results.size() );
		} );
	}

	@Test
	@JiraKey("HHH-12565")
	public void typeOfLeafTPC(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var results = session.createQuery(
					"select TYPE(f) " +
							"from Father f" +
							" where f.id = -1")
					.getResultList();
			Assertions.assertEquals( 0, results.size() );
		} );
	}

	@Test
	@JiraKey("HHH-12565")
	public void typeOfLeafTPCWithoutWhere(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var results = session.createQuery("select TYPE(f) from Father f")
					.getResultList();
			Assertions.assertEquals( 0, results.size() );
		} );
	}

	@Test
	@JiraKey("HHH-12565")
	public void typeOfParentInheritorTPCCriteriaTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Father father = new Father();
			father.id = 42L;
			session.persist( father );
			session.flush();

			HibernateCriteriaBuilder cb = session.getSessionFactory().getCriteriaBuilder();
			CriteriaQuery<Parent> query = cb.createQuery( Parent.class );
			Root<Parent> root = query.from( Parent.class );
			query.where( cb.equal( root.type(), cb.literal(Father.class) ) );
			List<Parent> result = session.createQuery( query ).getResultList();
			Assertions.assertEquals( result, List.of( father ) );
		} );
	}

	@Test
	@JiraKey("HHH-12565")
	public void typeOfTPCCriteriaTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			Father father = new Father();
			father.id = 42L;
			session.persist( father );
			session.flush();

			HibernateCriteriaBuilder cb = session.getSessionFactory().getCriteriaBuilder();
			CriteriaQuery<Class> query = cb.createQuery( Class.class );
			Root<Parent> root = query.from( Parent.class );
			query.select( root.type() );
			List<Class> result = session.createQuery( query ).getResultList();
			Assertions.assertEquals( List.of( Father.class ), result );
		} );
	}

	@Entity(name = "Child")
	public static class Child
	{
		@Id
		Long id;

		@ManyToOne
		Parent parent;

	}

	@Entity(name = "Father")
	public static class Father extends Parent {

		@Column
		String fathersDay;
	}

	@Entity(name = "Mother")
	public static class Mother extends Parent {

		@Column
		String mothersDay;
	}

	@Entity(name = "Parent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract static class Parent {
		@Id
		public Long id;
	}
}
