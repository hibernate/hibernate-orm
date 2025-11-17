/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneWithEmbeddedAndNotOptionalFieldTest.Client.class,
				ManyToOneWithEmbeddedAndNotOptionalFieldTest.User.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
@JiraKey("HHH-13705")
public class ManyToOneWithEmbeddedAndNotOptionalFieldTest {

	private Long userId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		User user = new User();
		scope.inTransaction(
				session -> {

					Log log = new Log();
					log.setCreationDate( OffsetDateTime.now() );

					Client client = new Client();
					client.setName( "And" );
					client.setLog( log );


					session.persist( client );

					user.setName( "Fab" );

					user.setClient( client );

					session.persist( user );
				}
		);
		userId = user.getId();
	}


	@Test
	public void load(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		scope.inTransaction(
				session -> {
					session.find( User.class, userId );
				}
		);
		assertThat( stats.getPrepareStatementCount(), is( 1L ) );
	}

	@Entity(name = "Client")
	public static class Client {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	public static class User {
		@Id
		@GeneratedValue
		private Long id;

		@Column(length = 120, nullable = false)
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idClient`")
		private Client client;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}
	}

	@Embeddable
	public static class Log {
		@Column(name = "`creationDate`", nullable = false)
		private OffsetDateTime creationDate;

		public OffsetDateTime getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(OffsetDateTime creationDate) {
			this.creationDate = creationDate;
		}
	}
}
