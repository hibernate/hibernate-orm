/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( value = OracleDialect.class )
public class TriggerGeneratedValuesWithoutCachingTest extends AbstractGeneratedPropertyTest {
	public final String[] getMappings() {
		return new String[] { "mapping/generated/GeneratedPropertyEntity.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}
}
