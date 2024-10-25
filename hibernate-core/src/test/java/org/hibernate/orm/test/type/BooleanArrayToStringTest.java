/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@DomainModel (
		annotatedClasses = {BooleanArrayToStringTest.TestEntity.class}
)
@SessionFactory
public class BooleanArrayToStringTest {

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayToString.class)
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	@Jira( value = "https://hibernate.atlassian.net/browse/HHH-18765" )
	public void testBooleanArrayToStringFunction(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> session.persist( new TestEntity(1L, new Boolean[]{Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE}) )
		);
		scope.inTransaction(
				session -> {
					String s = session.createQuery( "select array_to_string(t.theBoolean, ';', 'null') "
							+ "from TestEntity t", String.class ).getSingleResult();
					Assertions.assertEquals("false;false;null;true", s);
				}
		);
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;
		private Boolean[] theBoolean;

		public TestEntity() {
		}

		public TestEntity(Long id, Boolean[] theBoolean) {
			this.id = id;
			this.theBoolean = theBoolean;
		}
	}
}
