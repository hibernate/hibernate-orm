/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.instrument.buildtime;

import org.junit.Test;

import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.test.instrument.cases.Executable;
import org.hibernate.test.instrument.cases.TestCustomColumnReadAndWrite;
import org.hibernate.test.instrument.cases.TestDirtyCheckExecutable;
import org.hibernate.test.instrument.cases.TestFetchAllExecutable;
import org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable;
import org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable;
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

