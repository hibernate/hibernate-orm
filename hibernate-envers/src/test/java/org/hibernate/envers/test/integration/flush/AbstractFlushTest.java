/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.flush;

import javax.persistence.EntityManager;
import java.io.IOException;

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
