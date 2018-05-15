/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
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

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );

		if ( getAuditStrategy() != null ) {
			settings.put( EnversSettings.AUDIT_STRATEGY, getAuditStrategy() );
		}
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}
}
