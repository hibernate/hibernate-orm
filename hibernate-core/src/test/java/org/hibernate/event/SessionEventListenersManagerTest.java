/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;

public class SessionEventListenersManagerTest extends BaseUnitTestCase {

	@Test
	public void testListenerAppending() {
		StringBuilder sb = new StringBuilder();
		SessionEventListener a = new TestSessionEventListener( sb , 'a' );
		SessionEventListener b = new TestSessionEventListener( sb , 'b' );
		SessionEventListener c = new TestSessionEventListener( sb , 'c' );
		SessionEventListener d = new TestSessionEventListener( sb , 'd' );
		SessionEventListenerManagerImpl l = new SessionEventListenerManagerImpl( a, b );
		l.addListener( c, d );
		l.dirtyCalculationEnd( true );
		Assert.assertEquals( "abcd", sb.toString() );
		l.dirtyCalculationEnd( true );
		Assert.assertEquals( "abcdabcd", sb.toString() );
		l.addListener( new TestSessionEventListener( sb , 'e' ) );
		l.dirtyCalculationEnd( true );
		Assert.assertEquals( "abcdabcdabcde", sb.toString() );
	}

	@Test
	public void testEmptyListenerAppending() {
		StringBuilder sb = new StringBuilder();
		SessionEventListenerManagerImpl l = new SessionEventListenerManagerImpl();
		l.dirtyCalculationEnd( true );
		Assert.assertEquals( "", sb.toString() );
		l.addListener( new TestSessionEventListener( sb , 'e' ) );
		l.dirtyCalculationEnd( true );
		Assert.assertEquals( "e", sb.toString() );
	}

	private static class TestSessionEventListener extends BaseSessionEventListener {

		private final StringBuilder sb;
		private final char theChar;

		public TestSessionEventListener(StringBuilder sb, char theChar) {
			this.sb = sb;
			this.theChar = theChar;
		}

		//Just picking any method. This one is funny..
		@Override
		public void dirtyCalculationEnd(boolean dirty) {
			sb.append( theChar );
		}

	}

}
