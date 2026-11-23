package org.hibernate.orm.test.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replacing an element of a one-to-many collection with another entity, at the same map key or array
 * position, removes the old element from the collection and adds the new one. On the unowned side of the
 * association, it writes the map key column of the new element when the entity on the owning side has no
 * mapping for that column.
 */
@JiraKey("HHH-21240")
public abstract class AbstractOneToManyElementReplacementTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testReplaceValueOfMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			final Piece piece = new Piece( 1 );
			board.pieces.put( "a", piece );
			session.persist( piece );
			session.persist( board );
		} );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			final Piece piece = new Piece( 2 );
			session.persist( piece );
			assertThat( board.pieces.put( "a", piece ).id ).isEqualTo( 1 );
		} );

		assertThat( rows( scope, "select id, owner_id, slot from Piece order by id" ) )
				.containsExactly( row( 1, null, null ), row( 2, 1, "a" ) );
		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			assertThat( board.pieces ).containsOnlyKeys( "a" );
			assertThat( board.pieces.get( "a" ).id ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testReplaceValueOfInverseMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			final Cell cell = new Cell( 1, board );
			board.cells.put( "a", cell );
			session.persist( board );
			session.persist( cell );
		} );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			final Cell cell = new Cell( 2, board );
			session.persist( cell );
			final Cell replaced = board.cells.put( "a", cell );
			assertThat( replaced.id ).isEqualTo( 1 );
			replaced.board = null;
		} );

		assertThat( rows( scope, "select id, board_id, name from Cell where board_id is not null" ) )
				.containsExactly( row( 2, 1, "a" ) );
		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			assertThat( board.cells ).containsOnlyKeys( "a" );
			assertThat( board.cells.get( "a" ).id ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testReplaceElementOfArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			board.rooks = new Rook[] { new Rook( 1 ), new Rook( 2 ) };
			for ( Rook rook : board.rooks ) {
				session.persist( rook );
			}
			session.persist( board );
		} );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			final Rook rook = new Rook( 3 );
			session.persist( rook );
			board.rooks[0] = rook;
		} );

		assertThat( rows( scope, "select id, owner_id, pos from Rook order by id" ) )
				.containsExactly( row( 1, null, null ), row( 2, 1, 1 ), row( 3, 1, 0 ) );
		scope.inTransaction( session -> assertThat( session.find( Board.class, 1 ).rooks )
				.extracting( rook -> rook.id )
				.containsExactly( 3, 2 ) );
	}

	private static List<Object[]> rows(SessionFactoryScope scope, String sql) {
		return scope.fromTransaction( session -> session.createNativeQuery( sql, Object[].class ).getResultList() )
				.stream().map( AbstractOneToManyElementReplacementTest::withIntegers ).toList();
	}

	private static Object[] row(Object... values) {
		return values;
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

		@OneToMany
		@JoinColumn(name = "owner_id")
		@MapKeyColumn(name = "slot")
		Map<String, Piece> pieces = new HashMap<>();

		@OneToMany(mappedBy = "board")
		@MapKeyColumn(name = "name")
		Map<String, Cell> cells = new HashMap<>();

		@OneToMany
		@JoinColumn(name = "owner_id")
		@OrderColumn(name = "pos")
		Rook[] rooks;
	}

	@Entity(name = "Piece")
	public static class Piece {
		@Id
		Integer id;

		Piece() {
		}

		Piece(Integer id) {
			this.id = id;
		}
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

	@Entity(name = "Rook")
	public static class Rook {
		@Id
		Integer id;

		Rook() {
		}

		Rook(Integer id) {
			this.id = id;
		}
	}
}
