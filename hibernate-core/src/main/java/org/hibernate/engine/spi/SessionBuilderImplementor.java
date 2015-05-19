/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.SessionBuilder;

/**
 * Defines the internal contract between the <tt>SessionBuilder</tt> and other parts of
 * Hibernate..
 *
 * @see org.hibernate.SessionBuilder
 *
 * @author Gail Badner
 */
public interface SessionBuilderImplementor extends SessionBuilder {
	/**
	 * Adds the session owner to the session options
	 *
	 * @param sessionOwner The session owner.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionBuilder owner(SessionOwner sessionOwner);
}
