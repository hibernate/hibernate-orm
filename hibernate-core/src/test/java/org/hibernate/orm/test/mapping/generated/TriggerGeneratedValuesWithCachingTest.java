/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Implementation of TriggerGeneratedValuesWithoutCachingTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect( value = OracleDialect.class )
@DomainModel(xmlMappings = "org/hibernate/orm/test/mapping/generated/GeneratedPropertyEntity.hbm.xml")
public class TriggerGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {
}
