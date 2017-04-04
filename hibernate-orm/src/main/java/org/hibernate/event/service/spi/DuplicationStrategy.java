/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
