/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

/**
 * Generates increasing identifiers (in a single VM only).
 * Not valid across multiple VMs. Identifiers are not necessarily
 * strictly increasing, but usually are.
 */
public final class Timestamper {
	private static short counter = 0;
	private static long time;
	private static final int BIN_DIGITS = 12;
	public static final short ONE_MS = 1<<BIN_DIGITS;
	
	public static long next() {
		synchronized(Timestamper.class) {
			long newTime = System.currentTimeMillis() << BIN_DIGITS;
			if (time<newTime) {
				time = newTime;
				counter = 0;
			}
			else if (counter < ONE_MS - 1 ) {
				counter++;
			}
			
			return time + counter;
		}
	}

	private Timestamper() {}
}






