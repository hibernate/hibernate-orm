/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CachingAndVersionTest.Domain.class,
				CachingAndVersionTest.Server.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16126")
public class CachingAndVersionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Domain domain = new Domain();
					Server server = new Server();
					domain.addServer( server );

					session.persist( domain );
					session.persist( server );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Domain> domains =
							session.createQuery(
											"SELECT DISTINCT d FROM Domain d LEFT JOIN FETCH d.servers s",
											Domain.class
									)
									.getResultList();
					assertThat( domains.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Domain")
	@Table(name = "DOMAIN_TABLE")
	public static class Domain {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer rowVersion;

		@OneToMany(mappedBy = "domain")
		private Set<Server> servers = new HashSet<>();

		public Long getId() {
			return id;
		}

		public Integer getRowVersion() {
			return rowVersion;
		}

		public Set<Server> getServers() {
			return servers;
		}

		public void addServer(Server server) {
			servers.add( server );
			server.setDomain( this );
		}
	}

	@Entity(name = "Server")
	@Table(name = "SERVER_TABLE")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Server {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Domain domain;

		@Version
		private Integer rowVersion;

		public Long getId() {
			return id;
		}

		public Domain getDomain() {
			return domain;
		}

		public void setDomain(Domain domain) {
			this.domain = domain;
		}

		public Integer getRowVersion() {
			return rowVersion;
		}
	}
}
