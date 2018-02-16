/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.Collection;
import java.util.EnumSet;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public interface UsageDetails {
	boolean isUsedInFrom();
	boolean isUsedInSelect();
	boolean isUsedInWhere();

	// 	todo (6.0) : a `ManagedTypeValuedNavigable` would be better (to allow for composites)

	ManagedTypeDescriptor getIntrinsicSubclassIndicator();

	Collection<ManagedTypeDescriptor> getIncidentalSubclassIndicators();

	EnumSet<DowncastLocation> getDowncastLocations();

	Collection<Navigable> getReferencedNavigables();
}
