/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.strategy;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Storm.class
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				provider = StrategyTest.ImplicitNamyStrategyProvider.class
		)
)
public class StrategyTest {

	public static class ImplicitNamyStrategyProvider implements SettingProvider.Provider<ImplicitNamingStrategy> {
		@Override
		public ImplicitNamingStrategy getSetting() {
			return ImplicitNamingStrategyComponentPathImpl.INSTANCE;
		}
	}


	@Test
	public void testComponentSafeStrategy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Location start = new Location();
					start.setCity( "Paris" );
					start.setCountry( "France" );
					Location end = new Location();
					end.setCity( "London" );
					end.setCountry( "UK" );
					Storm storm = new Storm();
					storm.setEnd( end );
					storm.setStart( start );
					session.persist( storm );
					session.flush();
				}
		);
	}
}
