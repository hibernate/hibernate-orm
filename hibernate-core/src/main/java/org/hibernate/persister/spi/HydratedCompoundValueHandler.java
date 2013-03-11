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
package org.hibernate.persister.spi;

/**
 * Where to begin... :)
 *
 * This gets to the internal concept of 2-phase loading of entity data and how specifically it is done.  Essentially
 * for composite values, the process of hydration results in a tuple array comprising the composition "atomic" values.
 * For example, a Name component's hydrated state might look like {@code ["Steve", "L", "Ebersole"]}.
 *
 * There are times when we need to be able to extract individual pieces out of the hydrated tuple array.  For example,
 * for an entity with a composite identifier part of which is an association (a key-many-to-one) we often need to
 * attempt 2-phase processing on the association portion of the identifier's hydrated tuple array.
 *
 * This contract allows us access to portions of the hydrated tuple state.
 *
 * @author Steve Ebersole
 */
public interface HydratedCompoundValueHandler {
	public Object extract(Object hydratedState);
	public void inject(Object hydratedState, Object value);
}
