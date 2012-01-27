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
package org.hibernate.cache.spi.access;

import org.hibernate.cache.spi.NaturalIdRegion;

/**
 * Contract for managing transactional and concurrent access to cached naturalId
 * data.  For cached naturalId data, all modification actions actually just
 * invalidate the entry(s).  The call sequence here is:
 * {@link #lockItem} -> {@link #remove} -> {@link #unlockItem}
 * <p/>
 * There is another usage pattern that is used to invalidate entries
 * after performing "bulk" HQL/SQL operations:
 * {@link #lockRegion} -> {@link #removeAll} -> {@link #unlockRegion}
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Eric Dalquist
 */
public interface NaturalIdRegionAccessStrategy extends RegionAccessStrategy{

	/**
	 * Get the wrapped naturalId cache region
	 *
	 * @return The underlying region
	 */
	public NaturalIdRegion getRegion();
}
