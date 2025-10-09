/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test pagination on newer SQL Server Dialects where the application explicitly specifies
 * the legacy {@code SQLServerDialect} instead and will fail on pagination queries.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11642")
@RequiresDialect(SQLServerDialect.class)
@Jpa(
		annotatedClasses = { SQLServerDialectPaginationTest.SimpleEntity.class },
		settingProviders = { @SettingProvider(settingName = AvailableSettings.DIALECT, provider = SQLServerDialectPaginationTest.DialectSettingProvider.class) }
)
public class SQLServerDialectPaginationTest {

	static final class DialectSettingProvider implements SettingProvider.Provider<Dialect> {
		@Override
		public Dialect getSetting() {
			// if the environment is any version of SQLServerDialect, force the legacy SQLServerDialect instead
			// This is so that the legacy's TopLimitHandler will be used here to test the fix necessary when a
			// user explicitly configures the legacy dialect but uses a more modern version of SQL Server.
			final Dialect environmentDialect = DialectContext.getDialect();
			if ( environmentDialect instanceof SQLServerDialect ) {
				return new SQLServerDialect();
			}
			return environmentDialect;
		}
	}

	@Test
	public void testPaginationQuery(EntityManagerFactoryScope scope) {
		// prepare some test data
		scope.inTransaction( entityManager -> {
			for ( int i = 1; i <= 20; ++i ) {
				final SimpleEntity entity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( entity );
			}
		} );

		// This would fail with "index 2 out of range" within TopLimitHandler
		// The fix addresses this problem which only occurs when using SQLServerDialect explicitly.
		scope.inTransaction( entityManager -> {
			List<SimpleEntity> results = entityManager
					.createQuery( "SELECT o FROM SimpleEntity o WHERE o.id >= :firstId ORDER BY o.id", SimpleEntity.class )
					.setParameter( "firstId", 10 )
					.setMaxResults( 5 )
					.getResultList();
			// verify that the paginated query returned the right ids.
			final List<Integer> ids = results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() );
			assertEquals( Arrays.asList( 10, 11, 12, 13, 14 ), ids );
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity implements Serializable {
		@Id
		private Integer id;
		private String name;

		SimpleEntity() {}

		SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
