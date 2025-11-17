/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12561")
@DomainModel(
		annotatedClasses = {
				GlobalQuotedIdentifiersBulkIdTest.Person.class,
				GlobalQuotedIdentifiersBulkIdTest.Doctor.class,
				GlobalQuotedIdentifiersBulkIdTest.Engineer.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true"),
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
						provider = GlobalQuotedIdentifiersBulkIdTest.QueryMultyTableMutationStrategyProvider.class
				)
		}
)
public class GlobalQuotedIdentifiersBulkIdTest {

	public static class QueryMultyTableMutationStrategyProvider
			implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return InlineMutationStrategy.class.getName();
		}
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < entityCount(); i++ ) {
				Doctor doctor = new Doctor();
				doctor.setEmployed( (i % 2) == 0 );
				doctor.setEmployedOn( Timestamp.valueOf( "2018-06-01 00:00:00" ) );
				entityManager.persist( doctor );
			}

			for ( int i = 0; i < entityCount(); i++ ) {
				Engineer engineer = new Engineer();
				engineer.setEmployed( (i % 2) == 0 );
				engineer.setEmployedOn( Timestamp.valueOf( "2018-06-01 00:00:00" ) );
				engineer.setFellow( (i % 2) == 1 );
				entityManager.persist( engineer );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	protected int entityCount() {
		return 5;
	}

	@Test
	public void testBulkUpdate(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			int updateCount = entityManager.createQuery(
							"UPDATE Person u " +
							"SET u.employedOn = :date " +
							"WHERE u.id IN :userIds"
					)
					.setParameter( "date", Timestamp.valueOf( "2018-06-03 00:00:00" ) )
					.setParameter( "userIds", Arrays.asList( 1L, 2L, 3L ) )
					.executeUpdate();

			assertThat( updateCount ).isEqualTo( 3 );
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private boolean employed;

		@Temporal(TemporalType.TIMESTAMP)
		private Date employedOn;

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

		public Date getEmployedOn() {
			return employedOn;
		}

		public void setEmployedOn(Date employedOn) {
			this.employedOn = employedOn;
		}

		public boolean isEmployed() {
			return employed;
		}

		public void setEmployed(boolean employed) {
			this.employed = employed;
		}
	}

	@Entity(name = "Doctor")
	public static class Doctor extends Person {
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
	}
}
