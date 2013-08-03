/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.result;

/**
 * Represents the outputs of executing a JDBC statement accounting for mixing of result sets and update counts
 * hiding the complexity (IMO) of how this is exposed in the JDBC API.
 * <p/>
 * The outputs are exposed as a group of {@link Output} objects, each representing a single result set or update count.
 * Conceptually, Result presents those Returns as an iterator.
 *
 * @author Steve Ebersole
 */
public interface Outputs {
	/**
	 * Retrieve the current Output object.
	 *
	 * @return The current Output object.  Can be {@code null}
	 */
	public Output getCurrent();

	/**
	 * Go to the next Output object (if any), returning an indication of whether there was another (aka, will
	 * the next call to {@link #getCurrent()} return {@code null}?
	 *
	 * @return {@code true} if the next call to {@link #getCurrent()} will return a non-{@code null} value.
	 */
	public boolean goToNext();

	/**
	 * Eagerly release any resources held by this Outputs.
	 */
	public void release();
}
