/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile.internal;

/**
 * Commonality between entities and collections as something that can be affected by fetch profiles.
 *
 * @author Steve Ebersole
 */
public interface FetchProfileAffectee {
	/**
	 * Register the profile name with the entity/collection
	 */
	void registerAffectingFetchProfile(String fetchProfileName);
}
