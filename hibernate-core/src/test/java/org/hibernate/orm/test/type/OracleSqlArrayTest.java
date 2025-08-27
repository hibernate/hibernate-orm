/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = {OracleSqlArrayTest.Container.class})
@RequiresDialect(OracleDialect.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class OracleSqlArrayTest {

	@Test public void test(SessionFactoryScope scope) {
		Container container = new Container();
		container.activityKinds = new ActivityKind[] { ActivityKind.Work, ActivityKind.Play };
		container.bigIntegers = new BigInteger[] { new BigInteger("123"), new BigInteger("345") };
		scope.inTransaction( s -> s.persist( container ) );
		Container c = scope.fromTransaction( s-> s.createQuery("from ContainerWithArrays where bigIntegers = ?1", Container.class ).setParameter(1, new BigInteger[] { new BigInteger("123"), new BigInteger("345") }).getSingleResult() );
		assertArrayEquals( c.activityKinds, new ActivityKind[] { ActivityKind.Work, ActivityKind.Play } );
		assertArrayEquals( c.bigIntegers, new BigInteger[] { new BigInteger("123"), new BigInteger("345") } );
		c = scope.fromTransaction( s-> s.createQuery("from ContainerWithArrays where activityKinds = ?1", Container.class ).setParameter(1, new ActivityKind[] { ActivityKind.Work, ActivityKind.Play }).getSingleResult() );
	}

	@Test public void testSchema(SessionFactoryScope scope) {
		scope.inSession( s -> {
			Connection c;
			try {
				c = s.getJdbcConnectionAccess().obtainConnection();
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
			try {
				ResultSet tableInfo = c.getMetaData().getColumns(null, null, "CONTAINERWITHARRAYS", "BIGINTEGERS" );
				while ( tableInfo.next() ) {
					String type = tableInfo.getString(6);
					assertEquals( "BIGINTEGERBIGDECIMALARRAY", type );
					return;
				}
				fail("named array column not exported");
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
			finally {
				try {
					s.getJdbcConnectionAccess().releaseConnection(c);
				}
				catch (SQLException e) {
				}
			}
		});
		scope.inSession( s -> {
			Connection c;
			try {
				c = s.getJdbcConnectionAccess().obtainConnection();
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
			try {
				ResultSet tableInfo = c.getMetaData().getColumns(null, null, "CONTAINERWITHARRAYS", "ACTIVITYKINDS" );
				while ( tableInfo.next() ) {
					String type = tableInfo.getString(6);
					assertEquals( "ACTIVITYKINDBYTEARRAY", type );
					return;
				}
				fail("named array column not exported");
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
			finally {
				try {
					s.getJdbcConnectionAccess().releaseConnection(c);
				}
				catch (SQLException e) {
				}
			}
		});
	}

	public enum ActivityKind { Work, Play, Sleep }

	@Entity(name = "ContainerWithArrays")
	public static class Container {

		@Id @GeneratedValue Long id;

		@Array(length = 33)
		@Column(length = 25)
		@JdbcTypeCode(SqlTypes.ARRAY)
		BigInteger[] bigIntegers;

		@Array(length = 2)
		@JdbcTypeCode(SqlTypes.ARRAY)
		ActivityKind[] activityKinds;

	}

}
