/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound.exception;

import java.util.Set;

import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				NotFoundExceptionTest.ChessGame.class,
				NotFoundExceptionTest.ChessPlayer.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15229")
public class NotFoundExceptionTest {

	@AfterEach
	public void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNotFoundIgnore(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeMutationQuery(
										"INSERT INTO CHESS_GAME(id, player_black_id) VALUES (1, 1)" )
								.executeUpdate()

		);

		scope.inTransaction(
				session -> {
					ChessGame game = session.find( ChessGame.class, 1L );
					assertNotNull( game, "Returned entity shouldn't be null" );
					assertNull(
							game.getPlayerBlack(),
							"Broken foreign key reference with NotFoundAction.IGNORE should return null"
					);
					assertNull( game.getPlayerWhite() );
				}
		);

		scope.inTransaction(
				session -> {
					ChessGame chessGame = session.getReference( ChessGame.class, 1L );
					assertNotNull( chessGame );
					assertNull(
							chessGame.getPlayerBlack(),
							"Broken foreign key reference with NotFoundAction.IGNORE should return null"
					);
					assertNull( chessGame.getPlayerWhite() );
				}
		);
	}

	@Test
	public void testNotFoundIgnoreAndNotFoundException(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeMutationQuery(
										"INSERT INTO CHESS_GAME(id, player_white_id, player_black_id) VALUES (1, 1, 2)" )
								.executeUpdate()
		);

		Assertions.assertThrows(
				FetchNotFoundException.class, () -> scope.inTransaction(
						session ->
								session.find( ChessGame.class, 1L )
				)
		);

		Assertions.assertThrows(
				FetchNotFoundException.class, () -> scope.inTransaction(
						session -> {
							ChessGame chessGame = session.getReference( ChessGame.class, 1L );
							assertNotNull( chessGame );
							// this triggers ChessGame initialization, and because playerBlack cannot be found the FetchNotFoundException is thown
							assertNotNull( chessGame.getPlayerBlack() );
						}
				)
		);
	}

	@Test
	public void testNotFoundException(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeMutationQuery(
										"INSERT INTO CHESS_GAME(id, player_white_id) VALUES (1, 1)" )
								.executeUpdate()
		);

		Assertions.assertThrows(
				FetchNotFoundException.class, () -> scope.inTransaction(
						session ->
								session.find( ChessGame.class, 1L )
				)
		);

		Assertions.assertThrows(
				FetchNotFoundException.class, () -> scope.inTransaction(
						session -> {
							ChessGame chessGame = session.getReference( ChessGame.class, 1L );
							assertNotNull( chessGame );
							// this triggers ChessGame initialization, and because playerBlack cannot be found the FetchNotFoundException is thown
							assertNotNull( chessGame.getPlayerBlack() );
						}
				)
		);
	}

	@Entity(name = "ChessPlayer")
	@Table(name = "CHESS_PLAYER")
	public static class ChessPlayer {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "playerWhite")
		private Set<ChessGame> gamesWhite;

		@OneToMany(mappedBy = "playerBlack")
		private Set<ChessGame> gamesBlack;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<ChessGame> getGamesWhite() {
			return gamesWhite;
		}

		public void setGamesWhite(Set<ChessGame> gamesWhite) {
			this.gamesWhite = gamesWhite;
		}

		public Set<ChessGame> getGamesBlack() {
			return gamesBlack;
		}

		public void setGamesBlack(Set<ChessGame> gamesBlack) {
			this.gamesBlack = gamesBlack;
		}
	}

	@Entity(name = "ChessGame")
	@Table(name = "CHESS_GAME")
	public static class ChessGame {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "player_white_id")
		@NotFound(action = NotFoundAction.EXCEPTION)
		private ChessPlayer playerWhite;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "player_black_id")
		@NotFound(action = NotFoundAction.IGNORE)
		private ChessPlayer playerBlack;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ChessPlayer getPlayerWhite() {
			return playerWhite;
		}

		public void setPlayerWhite(ChessPlayer playerWhite) {
			this.playerWhite = playerWhite;
		}

		public ChessPlayer getPlayerBlack() {
			return playerBlack;
		}

		public void setPlayerBlack(ChessPlayer playerBlack) {
			this.playerBlack = playerBlack;
		}
	}
}
