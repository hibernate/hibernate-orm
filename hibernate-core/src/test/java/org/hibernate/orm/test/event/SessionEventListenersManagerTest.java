/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SessionEventListenersManagerTest {

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
		Assertions.assertEquals( "abcd", sb.toString() );
		l.dirtyCalculationEnd( true );
		Assertions.assertEquals( "abcdabcd", sb.toString() );
		l.addListener( new TestSessionEventListener( sb , 'e' ) );
		l.dirtyCalculationEnd( true );
		Assertions.assertEquals( "abcdabcdabcde", sb.toString() );
	}

	@Test
	public void testEmptyListenerAppending() {
		StringBuilder sb = new StringBuilder();
		SessionEventListenerManagerImpl l = new SessionEventListenerManagerImpl();
		l.dirtyCalculationEnd( true );
		Assertions.assertEquals( "", sb.toString() );
		l.addListener( new TestSessionEventListener( sb , 'e' ) );
		l.dirtyCalculationEnd( true );
		Assertions.assertEquals( "e", sb.toString() );
	}

	private static class TestSessionEventListener implements SessionEventListener {

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
