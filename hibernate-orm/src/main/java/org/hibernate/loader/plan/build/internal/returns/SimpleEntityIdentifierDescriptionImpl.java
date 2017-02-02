/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.plan.spi.EntityIdentifierDescription;

/**
 * @author Steve Ebersole
 */
public class SimpleEntityIdentifierDescriptionImpl implements EntityIdentifierDescription {
	@Override
	public boolean hasFetches() {
		return false;
	}

	@Override
	public boolean hasBidirectionalEntityReferences() {
		return false;
	}
}
