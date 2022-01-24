/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * Describes a model-part which contributes state to the array of values
 * for a container it is part of.  For example, an attribute contributes
 * a value to the state array for its declarer
 *
 * @author Steve Ebersole
 */
public interface StateArrayContributorMapping extends AttributeMapping, Fetchable {
	/**
	 * The attribute's position within the container's state array
	 */
	int getStateArrayPosition();

	@Override
	StateArrayContributorMetadataAccess getAttributeMetadataAccess();
}
