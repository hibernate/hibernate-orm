/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupDescriptor;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ast.HqlHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/// Verifies synthetic-table discovery while the standard SQM converter builds
/// grouping, ordering, and window expressions.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = SyntheticTableGroupSupportTests.Thing.class)
@ServiceRegistry(settings = {
		@Setting(
				name = AvailableSettings.DIALECT,
				value = "org.hibernate.orm.test.dialect.SyntheticTableGroupSupportTests$SyntheticTableDialect"
		),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory
public class SyntheticTableGroupSupportTests {
	@Test
	void tablelessOrderedLiteralGetsSyntheticRoot(SessionFactoryScope scope) {
		assertThat( translate( "select 1 order by 'order'", scope ) )
				.contains( "from (select 1) dummy_(x)" );
	}

	@Test
	void groupedLiteralGetsSyntheticRoot(SessionFactoryScope scope) {
		assertThat( translate( "select count(*) from Thing t group by 'group'", scope ) )
				.contains( "from Thing" )
				.contains( ",(select 1) dummy_(x)" );
	}

	@Test
	void windowOrderingGetsSyntheticRootForPartitionRendering(SessionFactoryScope scope) {
		assertThat( translate(
				"select row_number() over (partition by 'partition' order by 'order') from Thing t",
				scope
		) )
				.contains( "from Thing" )
				.contains( ",(select 1) dummy_(x)" );
	}

	@Test
	void pathOrderingDoesNotGetSyntheticRoot(SessionFactoryScope scope) {
		assertThat( translate( "select t.name from Thing t order by t.name", scope ) )
				.doesNotContain( "dummy_(x)" );
	}

	@Test
	void nestedQueryStateDoesNotLeak(SessionFactoryScope scope) {
		assertThat( translate(
				"select (select count(*) from Thing n group by 'nested') from Thing t order by t.name",
				scope
		) )
				.containsOnlyOnce( "dummy_(x)" );
	}

	@Test
	void descriptorRejectsBlankSqlFragments() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new SyntheticTableGroupDescriptor( " ", null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new SyntheticTableGroupDescriptor( "(select 1)", " " ) );
	}

	@Test
	void unresolvedExpressionDoesNotRequestSyntheticRoot() {
		assertThat( SyntheticTableGroupSupport.NONE.resolveSyntheticTableGroup( Clause.ORDER, null ) )
				.isNull();
		assertThat( SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS
				.resolveSyntheticTableGroup( Clause.ORDER, null ) )
				.isNull();
	}

	@Test
	void maintainedDialectsSelectFocusedStrategies() {
		assertThat( new H2Dialect().getSyntheticTableGroupSupport() )
				.isSameAs( SyntheticTableGroupSupport.NONE );
		assertThat( new SybaseDialect().getSyntheticTableGroupSupport() )
				.isSameAs( SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS );
	}

	private static String translate(String hql, SessionFactoryScope scope) {
		return HqlHelper.translateHql( hql, scope.getSessionFactory() ).sql();
	}

	public static class SyntheticTableDialect extends H2Dialect {
		@Override
		public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
			return SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS;
		}
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		private Long id;

		private String name;
	}
}
