/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.Transaction;
import org.hibernate.stat.NaturalIdStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.testing.cache.CachingRegionFactory.DEFAULT_ACCESSTYPE;
import static org.junit.Assert.*;

@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = DEFAULT_ACCESSTYPE, value = "nonstrict-read-write" ),
				@Setting(name = DISABLE_NATURAL_ID_RESOLUTIONS_CACHE, value = "true"),
				@Setting( name = GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( annotatedClasses = {A.class, Another.class, AllCached.class, B.class, SubClass.class} )
@SessionFactory
public class UnCachedMutableNaturalIdStrictReadWriteTest extends CachedMutableNaturalIdStrictTest {


}
