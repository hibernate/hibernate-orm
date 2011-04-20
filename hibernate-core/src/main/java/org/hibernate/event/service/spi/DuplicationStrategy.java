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
package org.hibernate.event.service.spi;

/**
 * Defines listener duplication checking strategy, both in terms of when a duplication is detected (see
 * {@link #areMatch}) as well as how to handle a duplication (see {@link #getAction}).
 *
 * @author Steve Ebersole
 */
public interface DuplicationStrategy {
	/**
	 * The enumerated list of actions available on duplication match
	 */
	public static enum Action {
		ERROR,
		KEEP_ORIGINAL,
		REPLACE_ORIGINAL
	}

	/**
	 * Are the two listener instances considered a duplication?
	 *
	 * @param listener The listener we are currently trying to register
	 * @param original An already registered listener
	 *
	 * @return {@literal true} if the two instances are considered a duplication; {@literal false} otherwise
	 */
	public boolean areMatch(Object listener, Object original);

	/**
	 * How should a duplication be handled?
	 *
	 * @return The strategy for handling duplication
	 */
	public Action getAction();
}
