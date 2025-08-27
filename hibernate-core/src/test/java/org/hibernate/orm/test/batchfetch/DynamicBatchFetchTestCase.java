/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;

@DomainModel(
		annotatedClasses = { Country.class, City.class }
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "15")
		}
)
public class DynamicBatchFetchTestCase {

	@Test
	@JiraKey(value = "HHH-12835")
	public void batchFetchTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Having DEFAULT_BATCH_FETCH_SIZE=15
			// results in batchSizes = [15, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
			// Let's create 11 countries so batch size 15 will be used with padded values,
			// this causes to have to remove 4 elements from list
			int numberOfCountries = 11;

			IntStream.range( 0, numberOfCountries ).forEach( i -> {
				Country c = new Country( "Country " + i );
				session.persist( c );
				session.persist( new City( "City " + i, c ) );
			} );
		} );

		scope.inTransaction( session -> {
			List<City> allCities = session.createQuery( "from City", City.class ).list();

			// this triggers countries to be fetched in batch
			assertNotNull( allCities.get( 0 ).getCountry().getName() );
		} );
	}
}
