/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integrator;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
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
		new Configuration().buildSessionFactory().close();
		new Configuration().addAnnotatedClass( SimpleNonAuditedEntity.class ).buildSessionFactory().close();
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
