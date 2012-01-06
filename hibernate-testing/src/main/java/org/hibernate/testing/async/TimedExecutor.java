/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.async;

import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TimedExecutor {
	private static final Logger log = Logger.getLogger( TimedExecutor.class );

	private final long timeOut;
	private final int checkMilliSeconds;

	public TimedExecutor(long timeOut) {
		this( timeOut, 1000 );
	}

	public TimedExecutor(long timeOut, int checkMilliSeconds) {
		this.timeOut = timeOut;
		this.checkMilliSeconds = checkMilliSeconds;
	}

	public void execute(Executable executable) throws TimeoutException {
		final ExecutableAdapter adapter = new ExecutableAdapter( executable );
		final Thread separateThread = new Thread( adapter );
		separateThread.start();

		int runningTime = 0;
		do {
			if ( runningTime > timeOut ) {
				try {
					executable.timedOut();
				}
				catch (Exception ignore) {
				}
				throw new TimeoutException();
			}
			try {
				Thread.sleep( checkMilliSeconds );
				runningTime += checkMilliSeconds;
			}
			catch (InterruptedException ignore) {
			}
		} while ( !adapter.isDone() );

		adapter.reThrowAnyErrors();
	}
}
