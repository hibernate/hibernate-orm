/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.spi.Fetchable;

/**
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
