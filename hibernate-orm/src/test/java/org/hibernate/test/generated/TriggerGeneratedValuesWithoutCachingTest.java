/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generated;

import org.hibernate.dialect.Oracle9iDialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( value = Oracle9iDialect.class )
public class TriggerGeneratedValuesWithoutCachingTest extends AbstractGeneratedPropertyTest {
	public final String[] getMappings() {
		return new String[] { "generated/GeneratedPropertyEntity.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}
}
