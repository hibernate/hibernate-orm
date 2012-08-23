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
package org.hibernate.engine.spi;

import java.io.Serializable;

/**
 * A contract for defining the aspects of cascading various persistence actions.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see CascadingAction
 */
public interface CascadeStyle extends Serializable {
	/**
	 * For this style, should the given action be cascaded?
	 *
	 * @param action The action to be checked for cascade-ability.
	 *
	 * @return True if the action should be cascaded under this style; false otherwise.
	 */
	public boolean doCascade(CascadingAction action);

	/**
	 * Probably more aptly named something like doCascadeToCollectionElements(); it is
	 * however used from both the collection and to-one logic branches...
	 * <p/>
	 * For this style, should the given action really be cascaded?  The default
	 * implementation is simply to return {@link #doCascade}; for certain
	 * styles (currently only delete-orphan), however, we need to be able to
	 * control this separately.
	 *
	 * @param action The action to be checked for cascade-ability.
	 *
	 * @return True if the action should be really cascaded under this style;
	 *         false otherwise.
	 */
	public boolean reallyDoCascade(CascadingAction action);

	/**
	 * Do we need to delete orphaned collection elements?
	 *
	 * @return True if this style need to account for orphan delete
	 *         operations; false otherwise.
	 */
	public boolean hasOrphanDelete();
}
