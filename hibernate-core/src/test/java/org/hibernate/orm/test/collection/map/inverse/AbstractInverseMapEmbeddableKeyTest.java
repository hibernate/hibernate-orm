package org.hibernate.orm.test.collection.map.inverse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The key columns of an inverse one-to-many map are written from the unowned side when the entity on the
 * owning side has no mapping for them. An embeddable key with a nullable property makes an update of the
 * map a recreation ({@code PersistentCollection#needsRecreate}), which must write them too.
 */
@JiraKey("HHH-21240")
public abstract class AbstractInverseMapEmbeddableKeyTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPutIntoLoadedMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Board() ) );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			for ( int i = 1; i <= 2; i++ ) {
				final Cell cell = new Cell( i, board );
				board.cells.put( new Coord( i, i ), cell );
				session.persist( cell );
			}
		} );

		assertThat( rows( scope ) ).containsExactly( row( 1, 1, 1 ), row( 2, 2, 2 ) );
		scope.inTransaction( session -> assertThat( session.find( Board.class, 1 ).cells.keySet() )
				.containsExactlyInAnyOrder( new Coord( 1, 1 ), new Coord( 2, 2 ) ) );
	}

	@Test
	public void testRekeyElementOfLoadedMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			session.persist( board );
			for ( int i = 1; i <= 2; i++ ) {
				final Cell cell = new Cell( i, board );
				board.cells.put( new Coord( i, i ), cell );
				session.persist( cell );
			}
		} );

		assertThat( rows( scope ) ).containsExactly( row( 1, 1, 1 ), row( 2, 2, 2 ) );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			board.cells.put( new Coord( 9, 9 ), board.cells.remove( new Coord( 1, 1 ) ) );
		} );

		assertThat( rows( scope ) ).containsExactly( row( 1, 9, 9 ), row( 2, 2, 2 ) );
		scope.inTransaction( session -> assertThat( session.find( Board.class, 1 ).cells.keySet() )
				.containsExactlyInAnyOrder( new Coord( 9, 9 ), new Coord( 2, 2 ) ) );
	}

	private static List<Object[]> rows(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> session
				.createNativeQuery( "select id, x, y from Cell order by id", Object[].class )
				.getResultList() )
				.stream().map( AbstractInverseMapEmbeddableKeyTest::withIntegers ).toList();
	}

	private static Object[] row(Integer id, Integer x, Integer y) {
		return new Object[] { id, x, y };
	}

	/**
	 * The Java type of an integer column read by a native query depends on the database.
	 */
	private static Object[] withIntegers(Object[] row) {
		final Object[] result = new Object[row.length];
		for ( int i = 0; i < row.length; i++ ) {
			result[i] = row[i] instanceof Number number ? number.intValue() : row[i];
		}
		return result;
	}

	@Entity(name = "Board")
	public static class Board {
		@Id
		Integer id = 1;

		@OneToMany(mappedBy = "board")
		Map<Coord, Cell> cells = new HashMap<>();
	}

	@Entity(name = "Cell")
	public static class Cell {
		@Id
		Integer id;

		@ManyToOne
		Board board;

		Cell() {
		}

		Cell(Integer id, Board board) {
			this.id = id;
			this.board = board;
		}
	}

	@Embeddable
	public static class Coord {
		Integer x;
		Integer y;

		Coord() {
		}

		Coord(Integer x, Integer y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof Coord coord
				&& Objects.equals( x, coord.x )
				&& Objects.equals( y, coord.y );
		}

		@Override
		public int hashCode() {
			return Objects.hash( x, y );
		}
	}
}
