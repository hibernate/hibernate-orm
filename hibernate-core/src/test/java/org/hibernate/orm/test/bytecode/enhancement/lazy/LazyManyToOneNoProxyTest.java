/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LazyManyToOneNoProxyTest.User.class,
				LazyManyToOneNoProxyTest.UserGroup.class,
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-16794")
public class LazyManyToOneNoProxyTest {

	private static final String USER_1_NAME = "Andrea";
	private static final String USER_2_NAME = "Fab";
	private static final String USER_GROUP_1_NAME = "group1";
	private static final String USER_GROUP_2_NAME = "group2";


	SQLStatementInspector statementInspector(SessionFactoryScope scope) {
		return (SQLStatementInspector) scope.getSessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					UserGroup group1 = new UserGroup( 1l, USER_GROUP_1_NAME );
					UserGroup group2 = new UserGroup( 2l, USER_GROUP_2_NAME );

					User user1 = new User( 1l, USER_1_NAME, group1 );
					User user2 = new User( 2l, USER_2_NAME, group2 );

					session.persist( user1 );
					session.persist( user2 );
					session.persist( group1 );
					session.persist( group2 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SQLStatementInspector statementInspector = statementInspector( scope );
					statementInspector.clear();

					User user = session.getReference( User.class, 1 );

					assertThat( user ).isNotNull();
					assertThat( user.getName() ).isEqualTo( USER_1_NAME );

					// User#team is lazy, so it should not be initialized
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );

					statementInspector.clear();

					UserGroup team = user.getTeam();
					assertThat( team.getName() ).isEqualTo( USER_GROUP_1_NAME );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "usr_tbl")
	public static class User {
		@Id
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "team_id")
		UserGroup team;

		public User() {
		}

		public User(Long id, String name, UserGroup team) {
			this.id = id;
			this.name = name;
			this.team = team;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public UserGroup getTeam() {
			return team;
		}
	}

	@Entity(name = "UserGroup")
	public static class UserGroup {
		@Id
		Long id;

		String name;

		public UserGroup() {
		}

		public UserGroup(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
