/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ElementCollectionAndCompositeKeyTest.OauthConnection.class
		}
)
@SessionFactory(exportSchema = false)
@JiraKey("HHH-17964")
@RequiresDialect( H2Dialect.class )
@RequiresDialect( PostgreSQLDialect.class )
public class ElementCollectionAndCompositeKeyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork(
								connection -> {
									PreparedStatement preparedStatement = connection.prepareStatement(
											"create table oauth_connection_grantedScopes (" +
													"        oauth_connection_connection varchar(255) not null," +
													"        oauth_connection_id uuid not null," +
													"        grantedScopes varchar(255)" +
													"    )" );
									try {
										preparedStatement.executeUpdate();
									}
									finally {
										preparedStatement.close();
									}


									preparedStatement = connection.prepareStatement(
											"create table oauth_connections (" +
													"        id uuid not null," +
													"        connection varchar(255) not null," +
													"        name varchar(255)," +
													"        primary key (connection, id)" +
													"    )" );
									try {
										preparedStatement.executeUpdate();
									}
									finally {
										preparedStatement.close();
									}
								}
						)
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork(
								connection -> {
									PreparedStatement preparedStatement = connection.prepareStatement(
											"drop table oauth_connection_grantedScopes " );
									try {
										preparedStatement.executeUpdate();
									}
									finally {
										preparedStatement.close();
									}
									preparedStatement = connection.prepareStatement(
											"drop table oauth_connections " );
									try {
										preparedStatement.executeUpdate();
									}
									finally {
										preparedStatement.close();
									}
								}
						)
		);
	}

	@Test
	public void testInitilizeElementCollection(SessionFactoryScope scope) {
		UUID primarySID = UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" );
		String connection = "def";
		OauthConnectionId id = new OauthConnectionId( primarySID, connection );
		scope.inTransaction(
				session -> {
					List<String> grantedScopes = new ArrayList<>();
					grantedScopes.add( "a" );
					grantedScopes.add( "b" );
					grantedScopes.add( "c" );
					OauthConnection oauthConnection = new OauthConnection( primarySID, connection, grantedScopes );
					session.persist( oauthConnection );
				}
		);

		scope.inTransaction(
				session -> {
					OauthConnection con = session.find(
							OauthConnection.class,
							id
					);
					List<String> grantedScopes = con.getGrantedScopes();
					grantedScopes.size();
				}
		);

		scope.inTransaction(
				session -> {
					OauthConnection con = session.createQuery(
							"select o from oauth_connection o where o.primarySID = :primarySID and o.connection = :connection",
							OauthConnection.class
					).setParameter( "primarySID", primarySID ).setParameter( "connection", connection ).uniqueResult();
					List<String> grantedScopes = con.getGrantedScopes();
					grantedScopes.size();
				}
		);

		scope.inTransaction(
				session -> {
					List<OauthConnection> connections = session.createQuery(
							"select o from oauth_connection o ",
							OauthConnection.class
					).list();
					assertThat( connections.size() ).isEqualTo( 1 );
					List<String> grantedScopes = connections.get( 0 ).getGrantedScopes();
					grantedScopes.size();
				}
		);

		scope.inTransaction(
				session -> {
					List<OauthConnection> connections = session.createQuery(
							"select o from oauth_connection o where o.id = :id",
							OauthConnection.class
					).setLockMode( LockModeType.PESSIMISTIC_WRITE ).setParameter( "id", id ).list();
					assertThat( connections.size() ).isEqualTo( 1 );
					List<String> grantedScopes = connections.get( 0 ).getGrantedScopes();
					grantedScopes.size();
				}
		);

	}

	@Entity(name = "oauth_connection")
	@Table(name = "oauth_connections")
	@IdClass(OauthConnectionId.class)
	public static class OauthConnection {

		@Id
		@Column(name = "id")
		@JdbcType(UUIDJdbcType.class)
		private UUID primarySID;

		@Id
		@Column(name = "connection")
		private String connection;

		@Column(name = "name")
		private String name;

		@ElementCollection
		private List<String> grantedScopes;

		public OauthConnection() {
		}

		public OauthConnection(UUID primarySID, String connection, List<String> grantedScopes) {
			this.primarySID = primarySID;
			this.connection = connection;
			this.grantedScopes = grantedScopes;
		}

		public UUID getPrimarySID() {
			return primarySID;
		}

		public String getConnection() {
			return connection;
		}

		public List<String> getGrantedScopes() {
			return grantedScopes;
		}
	}

	public static class OauthConnectionId {


		@Id
		@Column(name = "connection")
		private String connection;

		@Id
		@Column(name = "id")
		@JdbcType(UUIDJdbcType.class)
		private UUID primarySID;

		public OauthConnectionId() {
		}

		public OauthConnectionId(UUID primarySID, String connection) {
			this.primarySID = primarySID;
			this.connection = connection;
		}
	}

}
