/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Book.class,
		Umbrella.class,
		Sickness.class,
		Author.class,
		Article.class
})
public class GetIdentifierTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSimpleId(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			entityManager.persist( book );
			entityManager.flush();
			assertEquals( book.getId(), entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( book ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7561")
	public void testProxyObject(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			entityManager.persist( book );
			entityManager.flush();
			entityManager.clear(); // Clear persistence context to receive proxy object below.
			Book proxy = entityManager.getReference( Book.class, book.getId() );
			assertInstanceOf( HibernateProxy.class, proxy );
			assertEquals( book.getId(), entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( proxy ) );
		} );

		scope.inTransaction( entityManager -> {
			Author author = new Author();
			Article article = new Article( author );
			entityManager.persist( author );
			entityManager.persist( article );
			entityManager.flush();
			entityManager.clear(); // Clear persistence context to receive proxy relation below.
			article = entityManager.find( Article.class, article.getId() );
			assertInstanceOf( HibernateProxy.class, article.getAuthor() );
			assertEquals(
					author.getId(),
					entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( article.getAuthor() )
			);
		} );
	}

	@Test
	public void testEmbeddedId(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Umbrella umbrella = new Umbrella();
			umbrella.setId( new Umbrella.PK() );
			umbrella.getId().setBrand( "Burberry" );
			umbrella.getId().setModel( "Red Hat" );
			entityManager.persist( umbrella );
			entityManager.flush();
			assertEquals(
					umbrella.getId(),
					entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( umbrella )
			);
		} );
	}

	@Test
	public void testIdClass(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Sickness sick = new Sickness();
			sick.setClassification( "H1N1" );
			sick.setType( "Flu" );
			entityManager.persist( sick );
			entityManager.flush();
			Sickness.PK id = new Sickness.PK();
			id.setClassification( sick.getClassification() );
			id.setType( sick.getType() );
			assertEquals( id, entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( sick ) );
		} );
	}

}
