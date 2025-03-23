/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SessionFactory
@DomainModel
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-16650")
public class OracleNumericTypesTest {
	@Test void test(SessionFactoryScope scope) {
		String sql = "SELECT 012345678901234567890123456789, cast(1234567890123.456 as number(30,5)), cast(1234567890123.456 as double precision), cast(1234567890123.456 as float(32)), cast(1234567890123.456 as float(24)) from dual";
		Object[] values = (Object[]) scope.fromSession( s -> s.createNativeQuery( sql, Object[].class).getSingleResult());
		assertInstanceOf(BigDecimal.class, values[0]);
		assertInstanceOf(BigDecimal.class, values[1]);
		assertInstanceOf(BigDecimal.class, values[2]);
		assertInstanceOf(Double.class, values[3]);
		assertInstanceOf(Float.class, values[4]);
	}
}
