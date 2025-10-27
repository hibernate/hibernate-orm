/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Address_;
import org.hibernate.orm.test.jpa.metamodel.Article;
import org.hibernate.orm.test.jpa.metamodel.Article_;
import org.hibernate.orm.test.jpa.metamodel.EntityWithMapEC;
import org.hibernate.orm.test.jpa.metamodel.EntityWithMapEC_;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Translation;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;

import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Address.class, Phone.class, EntityWithMapEC.class, Article.class, Translation.class
})
public class PluralAttributeExpressionsTest {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IS [NOT] EMPTY

	@Test
	public void testCollectionIsEmptyHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select a from Address a where a.phones is empty" ).getResultList();
		});
	}

	@Test
	public void testCollectionIsEmptyCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
			final Root<Address> root = criteria.from( Address.class);

			criteria.select( root )
					.where( cb.isEmpty( root.get( Address_.phones ) ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testElementMapIsEmptyHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select m from EntityWithMapEC m where m.elements is empty" ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testElementMapIsEmptyCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<EntityWithMapEC> criteria = cb.createQuery( EntityWithMapEC.class );
			final Root<EntityWithMapEC> root = criteria.from( EntityWithMapEC.class);

			criteria.select( root )
					.where( cb.isMapEmpty( (JpaExpression) root.get( EntityWithMapEC_.elements ) ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testEntityMapIsEmptyHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select a from Article a where a.translations is empty" ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testEntityMapIsEmptyCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Article> criteria = cb.createQuery( Article.class );
			final Root<Article> root = criteria.from( Article.class);

			criteria.select( root )
					.where( cb.isEmpty( root.get( Article_.translations ) ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SIZE

	@Test
	public void testCollectionSizeHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select a from Address a where size(a.phones) > 1" ).getResultList();
		});
	}

	@Test
	public void testCollectionSizeCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
			final Root<Address> root = criteria.from( Address.class);

			criteria.select( root )
					.where( cb.gt( cb.size( root.get( Address_.phones ) ), 1 ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testElementMapSizeHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select m from EntityWithMapEC m where size( m.elements ) > 1" ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testElementMapSizeCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<EntityWithMapEC> criteria = cb.createQuery( EntityWithMapEC.class );
			final Root<EntityWithMapEC> root = criteria.from( EntityWithMapEC.class);

			criteria.select( root )
					.where( cb.gt( cb.mapSize( (JpaExpression) root.get( EntityWithMapEC_.elements ) ), 1 ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testEntityMapSizeHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "select a from Article a where size(a.translations) > 1" ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-11225")
	public void testEntityMapSizeCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Article> criteria = cb.createQuery( Article.class );
			final Root<Article> root = criteria.from( Article.class);

			criteria.select( root )
					.where( cb.gt( cb.mapSize( (JpaExpression) root.get( Article_.translations ) ), 1 ) );

			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19126")
	public void testPluralMapPathJavaType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Article> criteria = cb.createQuery( Article.class );
			final Root<Article> root = criteria.from( Article.class );

			assertThat( root.get( Article_.translations ).getJavaType() ).isEqualTo( Map.class );
			assertThat( root.get( "translations" ).getJavaType() ).isEqualTo( Map.class );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19126")
	public void testPluralListPathJavaType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
			final Root<Address> root = criteria.from( Address.class );

			assertThat( root.get( Address_.phones ).getJavaType() ).isEqualTo( List.class );
			assertThat( root.get( "phones" ).getJavaType() ).isEqualTo( List.class );
		} );
	}
}
