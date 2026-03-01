/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = CriteriaRestrictionTest.Doc.class)
class CriteriaRestrictionTest {
	@JiraKey( "HHH-19572" )
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Doc doc1 = new Doc();
					doc1.title = "Hibernate ORM";
					doc1.author = "Gavin King";
					doc1.text = "Hibernate ORM is a Java Persistence API implementation";
					entityManager.persist( doc1 );
					Doc doc2 = new Doc();
					doc2.title = "Hibernate ORM";
					doc2.author = "Hibernate Team";
					doc2.text = "Hibernate ORM is a Jakarta Persistence implementation";
					entityManager.persist( doc2 );
				}
		);
		scope.inTransaction(
				entityManager -> {
					var builder = entityManager.getCriteriaBuilder();
					var query = builder.createQuery( Doc.class );
					var d = query.from( Doc.class );
					// test with list
					query.where( List.of(
							builder.like( d.get( "title" ), "Hibernate%" ),
							builder.equal( d.get( "author" ), "Gavin King" )
					) );
					var resultList = entityManager.createQuery( query ).getResultList();
					assertEquals( 1, resultList.size() );
					assertEquals( "Hibernate ORM is a Java Persistence API implementation",
							resultList.get( 0 ).text );
				}
		);
		scope.inTransaction(
				entityManager -> {
					var builder = entityManager.getCriteriaBuilder();
					var query = builder.createQuery( Doc.class );
					var d = query.from( Doc.class );
					// test with varargs
					query.where(
							builder.like( d.get( "title" ), "Hibernate%" ),
							builder.equal( d.get( "author" ), "Hibernate Team" )
					);
					var resultList = entityManager.createQuery( query ).getResultList();
					assertEquals( 1, resultList.size() );
					assertEquals( "Hibernate ORM is a Jakarta Persistence implementation",
							resultList.get( 0 ).text );
				}
		);
	}
	@Entity(name = "Doc")
	static class Doc {
		@Id
		@GeneratedValue
		UUID uuid;
		private String title;
		private String author;
		private String text;
	}
}
