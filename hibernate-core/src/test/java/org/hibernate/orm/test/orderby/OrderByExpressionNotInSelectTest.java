/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orderby;

import java.util.List;

import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OrderByExpressionNotInSelectTest.Person.class,
		OrderByExpressionNotInSelectTest.Travel.class,
} )
@SessionFactory
public class OrderByExpressionNotInSelectTest {
	@Test
	public void testSimplePath(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// simple path
			session.createQuery( "select p.name from Person p order by p.age", String.class ).getResultList();
			// simple path multiple orders
			session.createQuery( "select p.name from Person p order by p.name, p.age", String.class ).getResultList();
			// function path
			session.createQuery( "select p.age from Person p order by upper(p.name)", Integer.class ).getResultList();
			// same path function sort
			session.createQuery( "select p.name from Person p order by upper(p.name)", String.class ).getResultList();
			// same path function selection
			session.createQuery( "select upper(p.name) from Person p order by p.name", String.class ).getResultList();
			// same function path selection and sort
			session.createQuery( "select upper(p.name) from Person p order by upper(p.name)", String.class ).getResultList();
			// different function path selection and sort
			session.createQuery( "select upper(p.name) from Person p order by lower(p.name)", String.class ).getResultList();
		} );
	}

	@Test
	public void testToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// select root and sort by path
			session.createQuery( "select p.age from Person p order by p.consort", Integer.class ).getResultList();
			// select and sort by implicit to-one
			session.createQuery( "select p.consort from Person p order by p.consort", Person.class ).getResultList();
			// select and sort by explicit to-one
			session.createQuery( "select c from Person p join p.consort c order by c", Person.class ).getResultList();
			// select and sort by explicit subpath
			session.createQuery( "select c.name from Person p join p.consort c order by c.age", String.class ).getResultList();
		} );
	}

	@Test
	public void testToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// order by whole element
			session.createQuery( "select p.age from Person p left join p.travels t order by element(t)", Integer.class ).getResultList();
			// order by subpath
			session.createQuery( "select p.name from Person p left join p.travels t order by element(t).destination", String.class ).getResultList();
			// select and order by different subpaths
			session.createQuery( "select element(t).id from Person p left join p.travels t order by element(t).destination", Integer.class ).getResultList();
		} );
	}

	@Test
	@SkipForDialect( dialectClass = SybaseDialect.class, reason = "Sybase doesn't support order by in a derived table", matchSubTypes = true )
	public void testSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// test order by in subquery
			session.createQuery( "select p.name from (select pers.name as name from Person pers order by pers.age limit 1) p", String.class ).getResultList();
			// test order by in main query
			session.createQuery( "select p.name from (select pers.name as name, pers.age as age from Person pers) p order by p.age", String.class ).getResultList();
		} );
	}

	@Entity( name = "Person" )
	static class Person {
		@Id
		private Long id;

		private Integer age;

		private String name;

		@ManyToOne
		private Person consort;

		@OneToMany
		private List<Travel> travels;
	}

	@Entity( name = "Travel" )
	static class Travel {
		@Id
		private Integer id;

		private String destination;
	}
}
