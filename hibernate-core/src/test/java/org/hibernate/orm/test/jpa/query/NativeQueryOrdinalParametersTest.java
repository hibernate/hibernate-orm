/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.Session;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {
				NativeQueryOrdinalParametersTest.Game.class,
				NativeQueryOrdinalParametersTest.Node.class
		}
)
public class NativeQueryOrdinalParametersTest {

	private static final String[] GAME_TITLES = { "Super Mario Brothers", "Mario Kart", "F-Zero" };

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					for ( String title : GAME_TITLES ) {
						Game game = new Game( title );
						entityManager.persist( game );
					}
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Game" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-10885")
	public void testNativeQueryIndexedOrdinalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( "SELECT * FROM GAME g WHERE title = ?1" );
					query.setParameter( 1, "Super Mario Brothers" );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10885")
	public void testNativeQueryOrdinalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( "SELECT * FROM GAME g WHERE title = ?" );
					query.setParameter( 1, "Super Mario Brothers" );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11121")
	public void testConflictWithSessionNativeQuery(EntityManagerFactoryScope scope) {
		final String sqlString = "SELECT * FROM GAME g WHERE title = ?";

		scope.inTransaction(
				entityManager -> {
					NativeQuery sqlQuery = entityManager.unwrap( Session.class ).createNativeQuery( sqlString );
					sqlQuery.setParameter( 1, "Super Mario Brothers" ).setCacheable( true );

					List results = sqlQuery.list();
					assertEquals( 1, results.size() );

					NativeQuery query = (NativeQuery) entityManager.createNativeQuery( sqlString );
					query.setParameter( 1, "Super Mario Brothers" );
					List list = query.list();
					assertEquals( 1, list.size() );

					sqlQuery = entityManager.unwrap( Session.class ).createNativeQuery( sqlString );
					sqlQuery.setParameter( 1, "Super Mario Brothers" ).setCacheable( true );

					results = sqlQuery.list();
					assertEquals( 1, results.size() );

					query.setParameter( 1, "Super Mario Brothers" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12532")
	// Add RequiresDialect be Cockroach version 201
	@RequiresDialect( value = PostgreSQLDialect.class )
	@RequiresDialect( value = CockroachDialect.class, majorVersion = 20, minorVersion = 1 )
	public void testCteNativeQueryOrdinalParameter(EntityManagerFactoryScope scope) {

		Node root1 = new Node();
		root1.setCode( "ABC" );
		Node root2 = new Node();
		root2.setCode( "DEF" );

		Node node11 = new Node();
		node11.setParent( root1 );

		Node node21 = new Node();
		node21.setParent( root2 );

		Node node211 = new Node();
		node211.setParent( node21 );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( root1 );
					entityManager.persist( root2 );

					entityManager.persist( node11 );
					entityManager.persist( node21 );

					entityManager.persist( node211 );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Query cte = entityManager.createNativeQuery(
							"WITH RECURSIVE CTE(id, parent_id) AS ( " +
									"  SELECT id, parent_id " +
									"  FROM Node " +
									"  WHERE code like ?1 and parent_id is null" +
									"  UNION ALL " +
									"  SELECT child.id, child.parent_id " +
									"  FROM Node child  " +
									"  JOIN CTE cte " +
									"  ON cte.id = child.parent_id " +
									") SELECT DISTINCT id as integer " +
									"  FROM CTE cte" );


					List<Long> root1Ids = cte.setParameter( 1, "AB%" ).getResultList();
					assertEquals( 2, root1Ids.size() );
					assertTrue( root1Ids.contains( root1.getId() ) );
					assertTrue( root1Ids.contains( node11.getId() ) );


					List<Long> root2Ids = cte.setParameter( 1, "DE%" ).getResultList();
					assertEquals( 3, root2Ids.size() );
					assertTrue( root2Ids.contains( root2.getId() ) );
					assertTrue( root2Ids.contains( node21.getId() ) );
					assertTrue( root2Ids.contains( node211.getId() ) );
				}
		);
	}

	@Entity(name = "Game")
	@Table(name = "GAME")
	public static class Game {
		private Long id;
		private String title;

		public Game() {
		}

		public Game(String title) {
			this.title = title;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@NotNull
		@Size(min = 3, max = 50)
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Node")
	@Table(name = "Node")
	public static class Node {

		@Id
		@GeneratedValue
		private Integer id;

		private String code;

		@ManyToOne(fetch = FetchType.LAZY)
		private Node parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}
	}
}
