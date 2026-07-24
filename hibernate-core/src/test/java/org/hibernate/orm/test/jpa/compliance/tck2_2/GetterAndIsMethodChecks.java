/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "12096")
public class GetterAndIsMethodChecks {

	@Test
	public void testIt() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory(
					MetadataBuildingTestHelper.buildMetadata( serviceRegistry, A.class )
			)
					.close();
		}
	}

	@Entity( name= "A" )
	public static class A {
		@Id
		private Integer id;
		@OneToOne
		private A b;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public boolean isB() {
			return true;
		}

		public A getB() {
			return b;
		}

		public void setB(A b) {
			this.b = b;
		}
	}
}
