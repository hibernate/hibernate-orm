/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
