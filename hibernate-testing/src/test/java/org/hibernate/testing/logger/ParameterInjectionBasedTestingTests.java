/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.logger;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.ServiceRegistry;
import org.hibernate.testing.orm.SessionFactory;
import org.hibernate.testing.orm.SessionFactoryScope;
import org.hibernate.testing.orm.TestDomain;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestDomain(
		standardModels = StandardDomainModel.GAMBIT
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class ParameterInjectionBasedTestingTests {
	@Test
	public void testSingleInjection(SessionFactoryScope scope) {
		assertThat( scope, notNullValue() );
	}

	@Test
	public void testMultipleInjections(SessionFactoryScope scope, MetadataImplementor model) {
		assertThat( scope, notNullValue() );
		assertThat( model, notNullValue() );
	}
}
