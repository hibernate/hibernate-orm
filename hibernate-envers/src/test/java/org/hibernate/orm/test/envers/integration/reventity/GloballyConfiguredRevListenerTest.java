/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.Map;
import jakarta.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6696")
public class GloballyConfiguredRevListenerTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.REVISION_LISTENER, CountingRevisionListener.class.getName() );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		CountingRevisionListener.revisionCount = 0;

		// Revision 1
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "data" );
		em.persist( te );
		em.getTransaction().commit();

		Assert.assertEquals( 1, CountingRevisionListener.revisionCount );
	}
}
