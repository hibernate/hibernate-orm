/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupDescriptor;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ast.HqlHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/// Verifies deterministic rejection of incompatible synthetic roots requested
/// while one SQL AST query specification is built.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = SyntheticTableGroupSupportTests.Thing.class)
@ServiceRegistry(settings = {
		@Setting(
				name = AvailableSettings.DIALECT,
				value = "org.hibernate.orm.test.dialect.SyntheticTableGroupConflictTests$ConflictingDialect"
		),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory
public class SyntheticTableGroupConflictTests {
	@Test
	void rejectsIncompatibleDescriptorsInOneQuerySpec(SessionFactoryScope scope) {
		assertThatIllegalStateException()
				.isThrownBy( () -> HqlHelper.translateHql(
						"select count(*) from Thing t group by 'group' order by 'order'",
						scope.getSessionFactory()
				) )
				.withMessageContaining( "incompatible descriptors" );
	}

	public static class ConflictingDialect extends H2Dialect {
		private static final SyntheticTableGroupDescriptor GROUP_ROOT =
				new SyntheticTableGroupDescriptor( "(select 1)", "group_root(x)" );
		private static final SyntheticTableGroupDescriptor ORDER_ROOT =
				new SyntheticTableGroupDescriptor( "(select 1)", "order_root(x)" );

		@Override
		public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
			return (clause, expression) -> switch ( clause ) {
				case GROUP -> GROUP_ROOT;
				case ORDER -> ORDER_ROOT;
				default -> null;
			};
		}
	}
}
