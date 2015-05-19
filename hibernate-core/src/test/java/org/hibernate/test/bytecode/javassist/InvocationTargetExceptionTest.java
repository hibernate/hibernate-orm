/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.javassist;

import java.text.ParseException;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.bytecode.Bean;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Test that the Javassist-based lazy initializer properly handles InvocationTargetExceptions
 *
 * @author Steve Ebersole
 */
@Skip(
		condition = InvocationTargetExceptionTest.LocalSkipMatcher.class,
		message = "environment not configured for javassist bytecode provider"
)
public class InvocationTargetExceptionTest extends BaseCoreFunctionalTestCase {
	public static class LocalSkipMatcher implements Skip.Matcher {
		@Override
		public boolean isMatch() {
			return ! BytecodeProviderImpl.class.isInstance( Environment.getBytecodeProvider() );
		}
	}

	@Override
	public String[] getMappings() {
		return new String[] { "bytecode/Bean.hbm.xml" };
	}

	@Test
	public void testProxiedInvocationException() {
		Session s = openSession();
		s.beginTransaction();
		Bean bean = new Bean();
		bean.setSomeString( "my-bean" );
		s.save( bean );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		bean = ( Bean ) s.load( Bean.class, bean.getSomeString() );
		assertFalse( Hibernate.isInitialized( bean ) );
		try {
			bean.throwException();
			fail( "exception not thrown" );
		}
		catch ( ParseException e ) {
			// expected behavior
		}
		catch ( Throwable t ) {
			fail( "unexpected exception type : " + t );
		}

		s.delete( bean );
		s.getTransaction().commit();
		s.close();
	}
}
