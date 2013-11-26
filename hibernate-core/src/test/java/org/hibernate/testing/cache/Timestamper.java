/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates increasing identifiers (in a single VM only). Not valid across multiple VMs.  Identifiers are not
 * necessarily strictly increasing, but usually are.
 *
 * Core while loop implemented by Alex Snaps - EHCache project - under ASL 2.0
 *
 * @author Hibernate team
 * @author Alex Snaps
 */
public final class Timestamper {
	private static final int BIN_DIGITS = 12;
	public static final short ONE_MS = 1 << BIN_DIGITS;
	private static final AtomicLong VALUE = new AtomicLong();

	public static long next() {
		while ( true ) {
			long base = System.currentTimeMillis() << BIN_DIGITS;
			long maxValue = base + ONE_MS - 1;

			for ( long current = VALUE.get(), update = Math.max( base, current + 1 ); update < maxValue;
				  current = VALUE.get(), update = Math.max( base, current + 1 ) ) {
				if ( VALUE.compareAndSet( current, update ) ) {
					return update;
				}
			}
		}
	}

	private Timestamper() {
	}
}






