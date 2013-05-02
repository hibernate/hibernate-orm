package org.hibernate.envers.test;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Strong Liu (stliu@hibernate.org)
 */
@RunWith(EnversRunner.class)
public abstract class BaseEnversFunctionalTestCase extends BaseCoreFunctionalTestCase {
	private String auditStrategy;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList(
				new Object[] {null},
				new Object[] {"org.hibernate.envers.strategy.ValidityAuditStrategy"}
		);
	}

	public void setTestData(Object[] data) {
		auditStrategy = (String) data[0];
	}

	public String getAuditStrategy() {
		return auditStrategy;
	}

	protected Session getSession() {
		if ( session == null || !session.isOpen() ) {
			return openSession();
		}
		return session;
	}

	protected AuditReader getAuditReader() {
		return AuditReaderFactory.get( getSession() );
	}

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		return configuration;
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}
}
