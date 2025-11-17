/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(SybaseDialect.class)
@RequiresDialect(SQLServerDialect.class)
@DomainModel(xmlMappings = "org/hibernate/orm/test/mapping/generated/MSSQLGeneratedPropertyEntity.hbm.xml")
public class TimestampGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {
}
