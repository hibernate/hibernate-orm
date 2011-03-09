/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.bytecode.cglib;
import java.lang.reflect.Field;

import org.hibernate.Session;
import org.hibernate.bytecode.cglib.BytecodeProviderImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.HibernateProxy;

import org.junit.Test;

import org.hibernate.testing.Skip;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.bytecode.ProxyBean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that the static thread local callback object is cleared out of the proxy class after instantiated.
 * This tests that the memory leak reported by HHH-2481 hasn't been re-introduced.
 *
 * @author Paul Malolepsy
 */
@TestForIssue(jiraKey = "HHH-2481")
public class CGLIBThreadLocalTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {"bytecode/Bean.hbm.xml"};
	}

	public static class LocalSkipCheck implements Skip.Matcher {
		@Override
		public boolean isMatch() {
			return !BytecodeProviderImpl.class.isInstance( Environment.getBytecodeProvider() );
		}
	}

	@Test
	@Skip(
			condition = LocalSkipCheck.class,
			message = "Environment not configured for CGLIB bytecode provider"
	)
	public void testCglibClearing() {
		//create the object for the test
		Session s = openSession();
		s.beginTransaction();
		ProxyBean proxyBean = new ProxyBean();
		proxyBean.setSomeString( "my-bean" );
		proxyBean.setSomeLong( 1234 );
		s.save( proxyBean );
		s.getTransaction().commit();
		s.close();

		// read the object as a proxy
		s = openSession();
		s.beginTransaction();
		proxyBean = (ProxyBean) s.load( ProxyBean.class, proxyBean.getSomeString() );
		assertTrue( proxyBean instanceof HibernateProxy );
		try {
			//check that the static thread callbacks thread local has been cleared out
			Field field = proxyBean.getClass().getDeclaredField( "CGLIB$THREAD_CALLBACKS" );
			field.setAccessible( true );
			ThreadLocal threadCallbacksThreadLocal = (ThreadLocal) field.get( null );
			assertTrue( threadCallbacksThreadLocal.get() == null );
		}
		catch (NoSuchFieldException e1) {
			fail( "unable to find CGLIB$THREAD_CALLBACKS field in proxy." );
		}
		catch (Throwable t) {
			fail( "unexpected exception type : " + t );
		}
		finally {
			//clean up
			s.delete( proxyBean );
			s.getTransaction().commit();
			s.close();
		}
	}
}
