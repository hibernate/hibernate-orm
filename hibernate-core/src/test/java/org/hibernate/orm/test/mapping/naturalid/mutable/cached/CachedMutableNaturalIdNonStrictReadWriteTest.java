/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;


import static org.hibernate.cfg.AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY;
import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.testing.cache.CachingRegionFactory.DEFAULT_ACCESSTYPE;

@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = DEFAULT_ACCESSTYPE, value = "nonstrict-read-write" ),
				@Setting( name = GENERATE_STATISTICS, value = "true" ),
				@Setting( name = ALLOW_REFRESH_DETACHED_ENTITY, value = "true" )
		}
)
@DomainModel( annotatedClasses = {A.class, Another.class, AllCached.class, B.class, SubClass.class} )
@SessionFactory
public class CachedMutableNaturalIdNonStrictReadWriteTest extends CachedMutableNaturalIdTest {

}
