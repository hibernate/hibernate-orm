/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;

import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.getDialect;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vladimir Klyushnikov
 * @author Hardy Ferentschik
 */
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "ddl")
)
@DomainModel(annotatedClasses = {
		Address.class,
		CupHolder.class,
		MinMax.class,
		DDLWithoutCallbackTest.RangeEntity.class
})
@SessionFactory
class DDLWithoutCallbackTest {

	@BeforeAll
	static void beforeAll(SessionFactoryScope scope) {
		// we want to get the SF built before we inspect the boot metamodel,
		// if we don't -- the integrators won't get applied, and hence DDL validation mode will not be applied either:
		scope.getSessionFactory();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsColumnCheck.class)
	void testListeners(SessionFactoryScope scope) {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		assertDatabaseConstraintViolationThrown( scope, ch );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsColumnCheck.class)
	public void testMinAndMaxChecksGetApplied(SessionFactoryScope scope) {
		MinMax minMax = new MinMax( 1 );
		assertDatabaseConstraintViolationThrown( scope, minMax );

		minMax = new MinMax( 11 );
		assertDatabaseConstraintViolationThrown( scope, minMax );

		final MinMax validMinMax = new MinMax( 5 );

		scope.inTransaction( s -> {
			s.persist( validMinMax );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsColumnCheck.class)
	public void testRangeChecksGetApplied(SessionFactoryScope scope) {
		RangeEntity range = new RangeEntity( 1 );
		assertDatabaseConstraintViolationThrown( scope, range );

		range = new RangeEntity( 11 );
		assertDatabaseConstraintViolationThrown( scope, range );

		RangeEntity validRange = new RangeEntity( 5 );

		scope.inTransaction( s -> {
			s.persist( validRange );
		} );
	}

	@Test
	public void testDDLEnabled(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Address.class.getName() );
		Column countryColumn = (Column) classMapping.getProperty( "country" ).getSelectables().get( 0 );
		assertThat( countryColumn.isNullable() ).as( "DDL constraints are not applied" ).isFalse();
	}

	private void assertDatabaseConstraintViolationThrown(SessionFactoryScope scope, Object o) {
		scope.inTransaction( session -> {
			try {
				session.persist( o );
				session.flush();
				fail( "expecting SQL constraint violation" );
			}
			catch (PersistenceException pe) {
				final Throwable cause = pe.getCause();
				if ( cause instanceof ConstraintViolationException ) {
					fail( "invalid object should not be validated" );
				}
				else if ( cause instanceof org.hibernate.exception.ConstraintViolationException ) {
					if ( getDialect().supportsColumnCheck() ) {
						// expected
					}
					else {
						org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) cause;
						fail( "Unexpected SQL constraint violation [" + cve.getConstraintName() + "] : " + cve.getSQLException() );
					}
				}
			}
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Entity(name = "RangeEntity")
	public static class RangeEntity {

		@Id
		@GeneratedValue
		private Long id;

		@org.hibernate.validator.constraints.Range(min = 2, max = 10)
		private Integer rangeProperty;

		private RangeEntity() {
		}

		public RangeEntity(Integer value) {
			this.rangeProperty = value;
		}
	}
}
