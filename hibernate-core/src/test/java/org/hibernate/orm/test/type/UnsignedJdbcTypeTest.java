/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.exception.DataException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HHH-14711 reported a bug (out-of-range) when using unsigned numeric types with native queries
 * we do not fix this, because
 * - a fix would be a breaking change
 * - using e.g. addScalar can be used as a simple workaround
 * - unsigned numeric types are not standard
 * note: the MariaDB implementation differs from MySQL
 */
@SessionFactory
@DomainModel(annotatedClasses = {UnsignedJdbcTypeTest.Account.class})
@JiraKey("HHH-14711")
public class UnsignedJdbcTypeTest {

	@Test
	@RequiresDialect(value = MariaDBDialect.class, matchSubTypes = false)
	void testUnsignedMariaDB(SessionFactoryScope scope) {

		scope.inTransaction( session -> {

			short balanceTinyint = 255;
			int balanceSmallint = 65535;
			int balanceMediumint = 16777215;
			long balanceInteger = 4294967295L;
			BigDecimal balanceBigint = new BigDecimal( "18446744073709551615" );

			session.createNativeMutationQuery( """
							insert into Account (id,balanceTinyint,balanceSmallint,balanceMediumint,
								balanceInteger,balanceBigint)
							values(1,:balanceTinyint,:balanceSmallint,:balanceMediumint,
								:balanceInteger,:balanceBigint)
							""" )
					.setParameter( "balanceTinyint", balanceTinyint )
					.setParameter( "balanceSmallint", balanceSmallint )
					.setParameter( "balanceMediumint", balanceMediumint )
					.setParameter( "balanceInteger", balanceInteger )
					.setParameter( "balanceBigint", balanceBigint )
					.executeUpdate();

			// what works with MariaDB
			assertEquals( balanceTinyint, session.createNativeQuery( "select balanceTinyint from Account" )
					.getSingleResult() );
			assertEquals( balanceSmallint, session.createNativeQuery( "select balanceSmallint from Account" )
					.getSingleResult() );
			assertEquals( balanceMediumint, session.createNativeQuery( "select balanceMediumint from Account" )
					.getSingleResult() );
			assertEquals( balanceInteger, session.createNativeQuery( "select balanceInteger from Account" )
					.getSingleResult() );

			// what doesn't work + workaround
			assertThrows( DataException.class, () -> session.createNativeQuery( "select balanceBigint from Account" )
					.getSingleResult() );
			assertEquals( balanceBigint, session.createNativeQuery( "select balanceBigint from Account" )
					.addScalar( "balanceBigint", StandardBasicTypes.BIG_DECIMAL )
					.getSingleResult() );
		} );
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	@SkipForDialect(dialectClass = MariaDBDialect.class)
	void testUnsignedMySQL(SessionFactoryScope scope) {

		scope.inTransaction( session -> {

			short balanceTinyint = 255;
			int balanceSmallint = 65535;
			int balanceMediumint = 16777215;
			long balanceInteger = 4294967295L;
			BigDecimal balanceBigint = new BigDecimal( "18446744073709551615" );

			session.createNativeMutationQuery( """
							insert into Account (id,balanceTinyint,balanceSmallint,balanceMediumint,
								balanceInteger,balanceBigint)
							values(1,:balanceTinyint,:balanceSmallint,:balanceMediumint,
								:balanceInteger,:balanceBigint)
							""" )
					.setParameter( "balanceTinyint", balanceTinyint )
					.setParameter( "balanceSmallint", balanceSmallint )
					.setParameter( "balanceMediumint", balanceMediumint )
					.setParameter( "balanceInteger", balanceInteger )
					.setParameter( "balanceBigint", balanceBigint )
					.executeUpdate();

			// what works with MySQL
			assertEquals( balanceMediumint, session.createNativeQuery( "select balanceMediumint from Account" )
					.getSingleResult() );

			// what doesn't work + workaround
			assertThrows( DataException.class, () -> session.createNativeQuery( "select balanceTinyint from Account" )
					.getSingleResult() );
			assertEquals( balanceTinyint, session.createNativeQuery( "select balanceTinyint from Account" )
					.addScalar( "balanceTinyint", StandardBasicTypes.SHORT )
					.getSingleResult() );

			assertThrows( DataException.class, () -> session.createNativeQuery( "select balanceSmallint from Account" )
					.getSingleResult() );
			assertEquals( balanceSmallint, session.createNativeQuery( "select balanceSmallint from Account" )
					.addScalar( "balanceSmallint", StandardBasicTypes.INTEGER )
					.getSingleResult() );

			assertThrows( DataException.class, () -> session.createNativeQuery( "select balanceInteger from Account" )
					.getSingleResult() );
			assertEquals( balanceInteger, session.createNativeQuery( "select balanceInteger from Account" )
					.addScalar( "balanceInteger", StandardBasicTypes.LONG )
					.getSingleResult() );

			assertThrows( DataException.class, () -> session.createNativeQuery( "select balanceBigint from Account" )
					.getSingleResult() );
			assertEquals( balanceBigint, session.createNativeQuery( "select balanceBigint from Account" )
					.addScalar( "balanceBigint", StandardBasicTypes.BIG_DECIMAL )
					.getSingleResult() );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		@GeneratedValue
		long id;

		@Column(columnDefinition = "tinyint unsigned")
		short balanceTinyint;

		@Column(columnDefinition = "smallint unsigned")
		short balanceSmallint;

		@Column(columnDefinition = "mediumint unsigned")
		short balanceMediumint;

		@Column(columnDefinition = "integer unsigned")
		long balanceInteger;

		@Column(columnDefinition = "bigint unsigned")
		long balanceBigint;
	}
}
