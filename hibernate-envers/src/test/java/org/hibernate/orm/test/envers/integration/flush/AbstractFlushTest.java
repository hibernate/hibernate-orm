/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.io.IOException;
import jakarta.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.junit.Before;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractFlushTest extends BaseEnversJPAFunctionalTestCase {
	public abstract FlushMode getFlushMode();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	private static Session getSession(EntityManager em) {
		Object delegate = em.getDelegate();
		if ( delegate instanceof Session ) {
			return (Session) delegate;
		}
		else if ( delegate instanceof EntityManager ) {
			Object delegate2 = ((EntityManager) delegate).getDelegate();

			if ( delegate2 instanceof Session ) {
				return (Session) delegate2;
			}
		}

		throw new RuntimeException( "Invalid entity manager" );
	}

	@Before
	public void initFlush() throws IOException {
		Session session = getSession( getEntityManager() );
		session.setHibernateFlushMode( getFlushMode() );
	}
}
