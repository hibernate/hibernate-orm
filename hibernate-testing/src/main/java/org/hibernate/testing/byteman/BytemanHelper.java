/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.byteman;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;
import org.jboss.logging.Logger;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class BytemanHelper extends Helper {
	private static final Logger log = Logger.getLogger( BytemanHelper.class );

	public static final AtomicInteger COUNTER = new AtomicInteger();

	protected BytemanHelper(Rule rule) {
		super( rule );
	}

	public void sleepASecond() {
		try {
			log.info( "Byteman rule triggered: sleeping a second" );
			Thread.sleep( 1000 );
		}
		catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			log.error( "unexpected interruption", e );
		}
	}

	public void throwNPE(String message) {
		//Needed because of Bug BYTEMAN-173: can't simply inject a NPE from the rule
		throw new NullPointerException( message );
	}

	public void countInvocation() {
		log.debug( "Increment call count" );
		COUNTER.incrementAndGet();
	}

	public static int getAndResetInvocationCount() {
		return COUNTER.getAndSet( 0 );
	}
}
