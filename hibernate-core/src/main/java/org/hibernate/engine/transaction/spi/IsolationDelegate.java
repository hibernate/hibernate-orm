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
package org.hibernate.engine.transaction.spi;

import org.hibernate.HibernateException;
import org.hibernate.jdbc.Work;

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
	 * @throws HibernateException Indicates a problem performing the work.
	 */
	public void delegateWork(Work work, boolean transacted) throws HibernateException;
}
