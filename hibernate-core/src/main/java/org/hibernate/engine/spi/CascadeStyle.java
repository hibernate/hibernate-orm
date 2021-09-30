/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
