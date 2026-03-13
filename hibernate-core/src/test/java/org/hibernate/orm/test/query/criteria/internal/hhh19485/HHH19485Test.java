/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh19485;


import jakarta.persistence.Tuple;
import org.hibernate.Session;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				Book.class,
				Isbn.class
		}
)
public class HHH19485Test {

	@BeforeAll
	public static void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Isbn isbn = new Isbn("123");
					Book book = new Book( 1, isbn );
					entityManager.persist( book );
					isbn = new Isbn("none");
					book = new Book( 2, isbn );
					entityManager.persist( book );
				}
		);
	}

	@AfterAll
	public static void teardown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void doTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					var cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
					JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
					JpaSubQuery<Tuple> subquery = query.subquery( Tuple.class );
					JpaRoot<Book> sqRoot = subquery.from( Book.class );

					JpaJoin<Book, Isbn> isbnJoin = sqRoot.join( Book_.isbn );
					JpaSimpleCase<String, String> isbnExpr = cb.<String, String>selectCase( isbnJoin.get( Isbn_.isbn) )
							.when( "none", "" )
							.otherwise( isbnJoin.get(Isbn_.isbn) );
					subquery.multiselect(
							sqRoot.get( Book_.id ).alias( "id" ),
							isbnExpr.alias( "isbn" )
					);
					JpaDerivedRoot<Tuple> root = query.from( subquery );
					query.select( cb.tuple(
							root.get( "id" ).alias( "id" ),
							root.get( "isbn" ).alias( "isbn" )
					)).orderBy(
							cb.asc( root.get("id") )
					);

					List<Tuple> list = entityManager.createQuery( query ).getResultList();
					assertEquals( 2, list.size() );
					Iterator<Tuple> it = list.iterator();
					Tuple tuple = it.next();
					assertEquals( 1, tuple.get("id") );
					assertEquals( "123", tuple.get("isbn") );
					tuple = it.next();
					assertEquals( 2, tuple.get("id") );
					assertEquals( "", tuple.get("isbn") );
				}
		);
	}

}
