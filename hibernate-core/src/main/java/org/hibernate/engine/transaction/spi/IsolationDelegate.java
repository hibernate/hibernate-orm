/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.spi;

import org.hibernate.HibernateException;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * Contract for performing work in a manner that isolates it from any current transaction.
 *
 * @author Steve Ebersole
 */
public interface IsolationDelegate {
	/**
	 * Perform the given work in isolation from current transaction.
	 *
	 * @param work The work to be performed.
	 * @param transacted Should the work itself be done in a (isolated) transaction?
	 *
	 * @return The work result
	 *
	 * @throws HibernateException Indicates a problem performing the work.
	 */
	public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException;
}
