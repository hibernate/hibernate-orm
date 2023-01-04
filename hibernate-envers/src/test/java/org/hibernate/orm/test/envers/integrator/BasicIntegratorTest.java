/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integrator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class BasicIntegratorTest {
	/**
	 * Tests that nothing crazy happens if the hibernate-envers jar happens
	 * to be on the classpath but we have no audited entities
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-9675" )
	public void testNoAudited() {
		new Configuration().buildSessionFactory(new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.CONNECTION_PROVIDER, SharedDriverManagerConnectionProviderImpl.getInstance())
				.build()).close();
		new Configuration().addAnnotatedClass( SimpleNonAuditedEntity.class ).buildSessionFactory(new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.CONNECTION_PROVIDER, SharedDriverManagerConnectionProviderImpl.getInstance())
				.build()).close();
	}

	@Entity
	public static class SimpleNonAuditedEntity {
		private Integer id;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
