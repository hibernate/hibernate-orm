/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.infrastructure;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests verifying parameter injection of test fixtures works
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class ParameterInjectionBasedTestingTests {
	@Test
	public void testSingleInjection(SessionFactoryScope scope) {
		assertThat( scope, notNullValue() );
	}

	@Test
	public void testSingleInjection(SessionFactoryImplementor sessionFactory) {
		assertThat( sessionFactory, notNullValue() );
	}

	@Test
	public void testSingleInjection(MetadataImplementor model) {
		assertThat( model, notNullValue() );
	}

	@Test
	public void testSingleInjection(StandardServiceRegistry registry) {
		assertThat( registry, notNullValue() );
	}

	@Test
	public void testMultipleInjections(SessionFactoryScope scope, MetadataImplementor model) {
		assertThat( scope, notNullValue() );
		assertThat( model, notNullValue() );
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "false"})
	public void testInjectionPlusParameterized(boolean bool, SessionFactoryScope scope) {
		assertThat( scope, notNullValue() );
	}

	@Test
	@RequiresDialect( DerbyTenSevenDialect.class )
	public void testRequiresDialect() {
		// should be disabled because of the filter
		fail( "Expecting test to be disabled due to @RequiresDialect" );
	}

	@Test
	@FailureExpected( "testing, testing, testing... 1, 2, 3" )
	public void testFailureExpected() {
		// should be disabled because of the filter
		fail( "Expecting test to be disabled due to @FailureExpected" );
	}
}
