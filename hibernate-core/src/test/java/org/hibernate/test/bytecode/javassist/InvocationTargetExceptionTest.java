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
package org.hibernate.test.bytecode.javassist;
import java.text.ParseException;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.Bean;
import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
