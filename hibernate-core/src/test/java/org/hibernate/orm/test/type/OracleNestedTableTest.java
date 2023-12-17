package org.hibernate.orm.test.type;

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
@DomainModel(annotatedClasses = {OracleNestedTableTest.Container.class})
@RequiresDialect(OracleDialect.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class OracleNestedTableTest {

	@Test public void test(SessionFactoryScope scope) {
		Container container = new Container();
		container.activityTypes = new ActivityType[] { ActivityType.Work, ActivityType.Play };
		container.strings = new String[] { "hello", "world" };
		scope.inTransaction( s -> s.persist( container ) );
		Container c = scope.fromTransaction( s-> s.createQuery("from ContainerWithArrays where strings = ?1", Container.class ).setParameter(1, new String[] { "hello", "world" }).getSingleResult() );
		assertArrayEquals( c.activityTypes, new ActivityType[] { ActivityType.Work, ActivityType.Play } );
		assertArrayEquals( c.strings, new String[] { "hello", "world" } );
		c = scope.fromTransaction( s-> s.createQuery("from ContainerWithArrays where activityTypes = ?1", Container.class ).setParameter(1, new ActivityType[] { ActivityType.Work, ActivityType.Play }).getSingleResult() );
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
				ResultSet tableInfo = c.getMetaData().getColumns(null, null, "CONTAINERWITHARRAYS", "STRINGS" );
				while ( tableInfo.next() ) {
					String type = tableInfo.getString(6);
					assertEquals( "STRINGARRAY", type );
					return;
				}
				fail("nested table column not exported");
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
				ResultSet tableInfo = c.getMetaData().getColumns(null, null, "CONTAINERWITHARRAYS", "ACTIVITYTYPES" );
				while ( tableInfo.next() ) {
					String type = tableInfo.getString(6);
					assertEquals( "ACTIVITYTYPEBYTEARRAY", type );
					return;
				}
				fail("nested table column not exported");
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

	public enum ActivityType { Work, Play, Sleep }

	@Entity(name = "ContainerWithArrays")
	public static class Container {

		@Id @GeneratedValue Long id;

		@Array(length = 33)
		@Column(length = 25)
		@JdbcTypeCode(SqlTypes.TABLE)
		String[] strings;

		@Array(length = 2)
		@JdbcTypeCode(SqlTypes.TABLE)
		ActivityType[] activityTypes;

	}

}
