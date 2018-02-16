/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public interface MutableUsageDetails extends UsageDetails {
	void usedInFromClause();

	void usedInSelectClause();

	void usedInOrderByClause();

	/**
	 * todo (6.0) : this could be used to handle implicit downcasts
	 */
	void addReferencedNavigable(Navigable navigable);

	/**
	 * todo (6.0) : a `ManagedTypeValuedNavigable` would be better (to allow for composites)
	 */
	void addDownCast(
			boolean intrinsic,
			IdentifiableTypeDescriptor downcastType,
			DowncastLocation downcastLocation);
}
