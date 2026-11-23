package org.hibernate.orm.test.collection.map;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The numeric keys of a one-to-many map are keys, and not the positions of its elements.
 */
@JiraKey("HHH-21240")
public abstract class AbstractOneToManyMapNumericKeyTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPutIntoLoadedMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			final Tile tile = new Tile( 1 );
			board.tiles.put( 10, tile );
			session.persist( tile );
			session.persist( board );
		} );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			final Tile tile = new Tile( 2 );
			session.persist( tile );
			board.tiles.put( 20, tile );
		} );

		assertThat( rows( scope, "select id, owner_id, pos from Tile order by id" ) )
				.containsExactly( row( 1, 1, 10 ), row( 2, 1, 20 ) );
		scope.inTransaction( session -> assertThat( session.find( Board.class, 1 ).tiles )
				.containsOnlyKeys( 10, 20 ) );
	}

	@Test
	public void testPutIntoLoadedInverseMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Board board = new Board();
			final Pawn pawn = new Pawn( 1, board );
			board.pawns.put( 10, pawn );
			session.persist( board );
			session.persist( pawn );
		} );

		scope.inTransaction( session -> {
			final Board board = session.find( Board.class, 1 );
			final Pawn pawn = new Pawn( 2, board );
			session.persist( pawn );
			board.pawns.put( 20, pawn );
		} );

		assertThat( rows( scope, "select id, board_id, pos from Pawn order by id" ) )
				.containsExactly( row( 1, 1, 10 ), row( 2, 1, 20 ) );
		scope.inTransaction( session -> assertThat( session.find( Board.class, 1 ).pawns )
				.containsOnlyKeys( 10, 20 ) );
	}

	private static List<Object[]> rows(SessionFactoryScope scope, String sql) {
		return scope.fromTransaction( session -> session.createNativeQuery( sql, Object[].class ).getResultList() );
	}

	private static Object[] row(Object... values) {
		return values;
	}

	@Entity(name = "Board")
	public static class Board {
		@Id
		Integer id = 1;

		@OneToMany
		@JoinColumn(name = "owner_id")
		@MapKeyColumn(name = "pos")
		Map<Integer, Tile> tiles = new HashMap<>();

		@OneToMany(mappedBy = "board")
		@MapKeyColumn(name = "pos")
		Map<Integer, Pawn> pawns = new HashMap<>();
	}

	@Entity(name = "Tile")
	public static class Tile {
		@Id
		Integer id;

		Tile() {
		}

		Tile(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Pawn")
	public static class Pawn {
		@Id
		Integer id;

		@ManyToOne
		Board board;

		Pawn() {
		}

		Pawn(Integer id, Board board) {
			this.id = id;
			this.board = board;
		}
	}
}
