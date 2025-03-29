/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
