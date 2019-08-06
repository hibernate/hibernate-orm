/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.mapping.spi.ModelPart;

/**
 * Contract for things that can be loaded by a Loader.
 *
 * Generally speaking this is limited to entities and collections
 *
 * @see Loader
 *
 * @author Steve Ebersole
 */
public interface Loadable extends ModelPart {
	boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers);
	boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers);
	boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers);
}
