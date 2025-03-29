/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.IntTestEntity;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Same as {@link org.hibernate.orm.test.envers.integration.basic.Simple}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaTransaction extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IntTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@Test
	@Priority(10)
	public void initData() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		EntityManager em;
		IntTestEntity ite;
		try {
			em = getEntityManager();
			ite = new IntTestEntity( 10 );
			em.persist( ite );
			id1 = ite.getId();
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
		}
		em.close();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		try {
			em = getEntityManager();
			ite = em.find( IntTestEntity.class, id1 );
			ite.setNumber( 20 );
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
		}
		em.close();
	}

	@Test
	public void testRevisionsCounts() throws Exception {
		Assert.assertEquals(
				2, getAuditReader().getRevisions(
				IntTestEntity.class, id1
		).size()
		);
	}

	@Test
	public void testHistoryOfId1() {
		IntTestEntity ver1 = new IntTestEntity( 10, id1 );
		IntTestEntity ver2 = new IntTestEntity( 20, id1 );

		List<Number> revisions = getAuditReader().getRevisions(
				IntTestEntity.class, id1
		);

		Assert.assertEquals(
				ver1, getAuditReader().find(
				IntTestEntity.class, id1, revisions.get( 0 )
		)
		);
		Assert.assertEquals(
				ver2, getAuditReader().find(
				IntTestEntity.class, id1, revisions.get( 1 )
		)
		);
	}
}
