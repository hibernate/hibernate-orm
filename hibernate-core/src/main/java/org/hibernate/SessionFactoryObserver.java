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
package org.hibernate;
import java.io.Serializable;

/**
 * Allows reaction to basic {@link SessionFactory} occurrences.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryObserver extends Serializable {
	/**
	 * Callback to indicate that the given factory has been created and is now ready for use.
	 *
	 * @param factory The factory initialized.
	 */
	public void sessionFactoryCreated(SessionFactory factory);

	/**
	 * Callback to indicate that the given factory has been closed.  Care should be taken
	 * in how (if at all) the passed factory reference is used since it is closed.
	 *
	 * @param factory The factory closed.
	 */
	public void sessionFactoryClosed(SessionFactory factory);
}
