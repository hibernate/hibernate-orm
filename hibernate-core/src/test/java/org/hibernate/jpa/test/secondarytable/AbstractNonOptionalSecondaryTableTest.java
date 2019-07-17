/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.secondarytable;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.runners.Parameterized;

public abstract class AbstractNonOptionalSecondaryTableTest extends BaseEntityManagerFunctionalTestCase {
	public enum JpaComplianceCachingSetting{ DEFAULT, TRUE, FALSE };

	private final JpaComplianceCachingSetting jpaComplianceCachingSetting;

	@Parameterized.Parameters(name = "JpaComplianceCachingSetting={0}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList(
				new Object[][] {
						{ JpaComplianceCachingSetting.DEFAULT },
						{ JpaComplianceCachingSetting.FALSE },
						{ JpaComplianceCachingSetting.TRUE }
				}
		);
	}

	AbstractNonOptionalSecondaryTableTest(JpaComplianceCachingSetting jpaComplianceCachingSetting) {
		this.jpaComplianceCachingSetting = jpaComplianceCachingSetting;
	}

	@Override
	protected void addConfigOptions(Map options) {
		switch ( jpaComplianceCachingSetting ) {
			case DEFAULT:
				// Keep the default (false)
				break;
			case TRUE:
				options.put(
						AvailableSettings.JPA_CACHING_COMPLIANCE,
						Boolean.TRUE
				);
				break;
			case FALSE:
				options.put(
						AvailableSettings.JPA_CACHING_COMPLIANCE,
						Boolean.FALSE
				);
				break;
		}
	}
}