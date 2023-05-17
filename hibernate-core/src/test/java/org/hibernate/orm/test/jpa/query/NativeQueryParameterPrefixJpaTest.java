/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				NativeQueryParameterPrefixJpaTest.Game.class,
				NativeQueryParameterPrefixJpaTest.Node.class
		}
)
public class NativeQueryParameterPrefixJpaTest {

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
	public void testNativeQueryIndexedOrdinalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( "SELECT * FROM GAME g WHERE title = @1" );
					query.setHint(HibernateHints.HINT_ORDINAL_PARAMETER_PREFIX, '@')
							.setParameter( 1, "Super Mario Brothers" );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testNativeQueryNamedParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( "SELECT * FROM GAME g WHERE title = #t" );
					query.setHint(HibernateHints.HINT_NAMED_PARAMETER_PREFIX, '#')
							.setParameter( "t", "Super Mario Brothers" );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
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
