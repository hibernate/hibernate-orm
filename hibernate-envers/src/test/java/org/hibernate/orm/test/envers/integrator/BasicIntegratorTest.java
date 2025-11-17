/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integrator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class BasicIntegratorTest {
	/**
	 * Tests that nothing crazy happens if the hibernate-envers jar happens
	 * to be on the classpath but we have no audited entities
	 */
	@Test
	@JiraKey( value = "HHH-9675" )
	public void testNoAudited() {
		new Configuration().buildSessionFactory( ServiceRegistryUtil.serviceRegistry()).close();
		new Configuration().addAnnotatedClass( SimpleNonAuditedEntity.class ).buildSessionFactory(ServiceRegistryUtil.serviceRegistry()).close();
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
