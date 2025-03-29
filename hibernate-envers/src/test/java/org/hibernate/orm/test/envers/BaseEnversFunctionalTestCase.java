/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.internal.SessionImpl;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import org.hibernate.service.ServiceRegistry;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Strong Liu (stliu@hibernate.org)
 */
@RunWith(EnversRunner.class)
public abstract class BaseEnversFunctionalTestCase extends BaseNonConfigCoreFunctionalTestCase {
	private String auditStrategy;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList(
				new Object[]{ null },
				new Object[]{ "org.hibernate.envers.strategy.ValidityAuditStrategy" }
		);
	}

	public void setTestData(Object[] data) {
		auditStrategy = (String) data[0];
	}

	public String getAuditStrategy() {
		return auditStrategy;
	}

	@Override
	protected Session getSession() {
		Session session = super.getSession();
		if ( session == null || !session.isOpen() ) {
			return openSession();
		}
		return session;
	}

	protected AuditReader getAuditReader() {
		Session session = getSession();
		if(session.getTransaction().getStatus() != TransactionStatus.ACTIVE ){
			session.getTransaction().begin();
		}

		return AuditReaderFactory.get( getSession() );
	}

	protected Configuration getConfiguration() {
		ServiceRegistry registry = getSession().unwrap(SessionImpl.class ).getSessionFactory().getServiceRegistry();
		return registry.getService( EnversService.class ).getConfig();
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		// Envers tests expect sequences to not skip values...
		settings.put( EnversSettings.REVISION_SEQUENCE_NOCACHE, "true" );

		if ( getAuditStrategy() != null ) {
			settings.put( EnversSettings.AUDIT_STRATEGY, getAuditStrategy() );
		}
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}
}
