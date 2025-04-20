/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				ElementCollectionWithSubselectFetchModeTest.Account.class,
				ElementCollectionWithSubselectFetchModeTest.Client.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-15640")
public class ElementCollectionWithSubselectFetchModeTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Account account = new Account( 1l, "first account" );
					Set<Long> zids = new HashSet<>();
					zids.add( 1l );
					zids.add( 2l );
					Client client = new Client( 1l, "fab", zids, account );
					session.persist( account );
					session.persist( client );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {

					Query<Client> query = session.createQuery(
							"from " + Client.class.getName() + " c where c.name='fab'",
							Client.class
					);
					Client client = query.getSingleResult();

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 3 );

					assertThat( client.id ).isEqualTo( 1L );
					assertTrue( Hibernate.isPropertyInitialized( client, "zids" ) );
				}
		);
	}

	@Entity(name = "Client")
	@Table(name = "CLIENT_TABLE")
	public static class Client {
		@Id
		private Long id;

		private String name;

		@CollectionTable(
				name = "zid_ids",
				joinColumns = @JoinColumn(name = "id_client", referencedColumnName = "id"))
		@Column(name = "id")
		@ElementCollection(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		private Set<Long> zids = new HashSet<>();

		@OneToOne(orphanRemoval = true)
		@JoinColumn(name = "id_account", nullable = false)
		private Account account;

		public Client() {
		}

		public Client(Long id, String name, Set<Long> zids, Account account) {
			this.id = id;
			this.name = name;
			this.zids = zids;
			this.account = account;
		}
	}

	@Entity(name = "Account")
	@Table(name = "ACCOUNT_TABLE")
	public static class Account {

		@Id
		private Long id;

		private String name;

		public Account() {
		}

		public Account(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
