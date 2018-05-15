/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.flush;

import java.io.IOException;
import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.StrTestEntity;

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
		session.setFlushMode( getFlushMode() );
	}
}
