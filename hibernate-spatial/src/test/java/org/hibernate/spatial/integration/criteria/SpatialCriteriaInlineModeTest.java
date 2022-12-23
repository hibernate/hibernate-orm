/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration.criteria;

import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

/**
 * Test SpatialCriteria queries using geometric parameter inlining
 *
 * @author Marco Belladelli
 */
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "INLINE"),
		@Setting(name = HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT, value = "true")
})
@SessionFactory
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "See https://hibernate.atlassian.net/browse/HHH-15669")
public class SpatialCriteriaInlineModeTest extends SpatialCriteriaTest {
	@Override
	public Stream<PredicateRegexes.PredicateRegex> getTestRegexes() {
		return super.predicateRegexes.inlineModeRegexes();
	}
}
