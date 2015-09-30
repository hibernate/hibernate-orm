/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.buildtime;

import org.junit.Test;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.test.instrument.cases.Executable;
import org.hibernate.test.instrument.cases.TestCustomColumnReadAndWrite;
import org.hibernate.test.instrument.cases.TestDirtyCheckExecutable;
import org.hibernate.test.instrument.cases.TestFetchAllExecutable;
import org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable;
import org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable;
import org.hibernate.test.instrument.cases.TestLazyBasicFieldAccessExecutable;
import org.hibernate.test.instrument.cases.TestLazyBasicPropertyAccessExecutable;
import org.hibernate.test.instrument.cases.TestLazyExecutable;
import org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable;
import org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable;
import org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable;
import org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Gavin King
 */
@Skip(
		message = "domain classes not instrumented for build-time instrumentation testing",
		condition = InstrumentTest.SkipCheck.class
)
public class InstrumentTest extends BaseUnitTestCase {
	@Test
	public void testDirtyCheck() throws Exception {
		execute( new TestDirtyCheckExecutable() );
	}

	@Test
	public void testFetchAll() throws Exception {
		execute( new TestFetchAllExecutable() );
	}

	@Test
	public void testLazy() throws Exception {
		execute( new TestLazyExecutable() );
	}

	@Test
	public void testLazyManyToOne() throws Exception {
		execute( new TestLazyManyToOneExecutable() );
	}

	@Test
	public void testSetFieldInterceptor() throws Exception {
		execute( new TestInjectFieldInterceptorExecutable() );
	}

	@Test
	public void testPropertyInitialized() throws Exception {
		execute( new TestIsPropertyInitializedExecutable() );
	}

	@Test
	public void testManyToOneProxy() throws Exception {
		execute( new TestManyToOneProxyExecutable() );
	}

	@Test
	public void testLazyPropertyCustomTypeExecutable() throws Exception {
		execute( new TestLazyPropertyCustomTypeExecutable() );
	}

	@Test
	public void testLazyBasicFieldAccess() throws Exception {
		execute( new TestLazyBasicFieldAccessExecutable() );
	}

	@Test
	public void testLazyBasicPropertyAccess() throws Exception {
		execute( new TestLazyBasicPropertyAccessExecutable() );
	}

	@Test
	public void testSharedPKOneToOne() throws Exception {
		execute( new TestSharedPKOneToOneExecutable() );
	}

	@Test
	public void testCustomColumnReadAndWrite() throws Exception {
		execute( new TestCustomColumnReadAndWrite() );
	}	
	
	private void execute(Executable executable) throws Exception {
		executable.prepare();
		try {
			executable.execute();
		}
		finally {
			executable.complete();
		}
	}

	public static class SkipCheck implements Skip.Matcher {
		@Override
		public boolean isMatch() {
			return ! FieldInterceptionHelper.isInstrumented( new Document() );
		}
	}
}

