package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
// Make sure this stuff runs on a dedicated connection pool,
// otherwise we might run into ORA-21700: object does not exist or is marked for delete
// because the JDBC connection or database session caches something that should have been invalidated
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = ""))
@DomainModel(annotatedClasses = {OracleNestedTableTest.Container.class})
@RequiresDialect(OracleDialect.class)
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
					assertEquals( "ACTIVITYTYPEARRAY", type );
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
