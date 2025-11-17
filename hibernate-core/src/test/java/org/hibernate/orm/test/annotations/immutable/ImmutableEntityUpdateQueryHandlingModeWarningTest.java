/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;


import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.cfg.QuerySettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12387")
@DomainModel(
		annotatedClasses = {
				Country.class,
				State.class,
				Photo.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE, value = "warning")
)
public class ImmutableEntityUpdateQueryHandlingModeWarningTest {

	@RegisterExtension
	public LoggerInspectionExtension logInspection =
			LoggerInspectionExtension.builder().setLogger( CORE_LOGGER ).build();

	@Test
	public void testBulkUpdate(SessionFactoryScope scope) {
		Country _country = scope.fromTransaction( session -> {
			Country country = new Country();
			country.setName( "Germany" );
			session.persist( country );
			return country;
		} );

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000487" );
		triggerable.reset();

		scope.inTransaction( session -> {
			session.createMutationQuery(
							"update Country " +
							"set name = :name" )
					.setParameter( "name", "N/A" )
					.executeUpdate();
		} );

		assertThat( triggerable.triggerMessage() )
				.isEqualTo(
						"HHH000487: The query [update Country set name = :name] updates an immutable entity: [Country]" );

		scope.inTransaction( session -> {
			Country country = session.find( Country.class, _country.getId() );
			assertThat( country.getName() ).isEqualTo( "N/A" );
		} );
	}
}
