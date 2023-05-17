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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(
		annotatedClasses = {
				NativeQueryParameterPrefixTest.Game.class,
				NativeQueryParameterPrefixTest.Node.class
		}
)
public class NativeQueryParameterPrefixTest {

	private static final String[] GAME_TITLES = { "Super Mario Brothers", "Mario Kart", "F-Zero" };

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( String title : GAME_TITLES ) {
						Game game = new Game( title );
						session.persist( game );
					}
				}
		);
	}

	@BeforeEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeMutationQuery("delete GAME").executeUpdate();
				}
		);
	}

	@Test
	public void testNativeQueryIndexedOrdinalParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery( "SELECT * FROM GAME g WHERE title = @1" );
					query.setParameterEscapes('#','@')
							.setParameter( 1, "Super Mario Brothers" );
					List list = query.getResultList();
					assertEquals( 1, list.size() );
				}
		);
	}

	@Test
	public void testNativeQueryNamedParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery( "SELECT * FROM GAME g WHERE title = #t" );
					query.setParameterEscapes('#','@')
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
