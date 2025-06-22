/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.query;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;

import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@SuppressWarnings("unchecked")
public class StrictJpaComplianceTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.JPA_QUERY_COMPLIANCE, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Organization.class
		};
	}

	@Test
	public void testIt() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			AuditReaderFactory.get( entityManager ).getRevisions( Organization.class, 1 );
		} );

	}

	/**
	 * @author Madhumita Sadhukhan
	 */
	@Entity
	@Table(name = "ORG")
	public static class Organization {

		@Id
		@GeneratedValue
		@Audited
		private int id;

		@Audited
		@Column(name = "ORG_NAME")
		private String name;

		public Organization() {
		}

	}
}
