/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.query.NavigablePath;

/**
 * An entity fetch (many-to-one, one-to-one)
 *
 * @author Steve Ebersole
 */
public interface EntityFetch extends EntityMappingNode, Fetch {
	default NavigablePath getNavigablePath() {
		return getFetchParent().getNavigablePath().append( getFetchedNavigable().getNavigableName() );
	}
}
