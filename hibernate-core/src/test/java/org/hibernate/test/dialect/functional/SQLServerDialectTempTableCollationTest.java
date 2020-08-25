/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Nathan Xu
 */
@RequiresDialect( SQLServer2005Dialect.class )
@TestForIssue( jiraKey = "HHH-3326" )
public class SQLServerDialectTempTableCollationTest extends BaseCoreFunctionalTestCase {

	private String originalDBCollation;
	private final String changedDBCollation = "SQL_Latin1_General_CP437_BIN";
	private boolean collationChanged;

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, Boolean.TRUE.toString() );
		return configuration;
	}

	@AfterClassOnce
	protected void revertBackOriginalDBCollation() {
		if ( originalDBCollation != null && collationChanged && !changedDBCollation.equals( originalDBCollation ) ) {
			BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
			StandardServiceRegistryImpl serviceRegistry = buildServiceRegistry(
					bootRegistry,
					constructConfiguration()
			);
			try (Connection connection = serviceRegistry.getService( JdbcServices.class )
					.getBootstrapJdbcConnectionAccess()
					.obtainConnection();
				 Statement statement = connection.createStatement()) {
				connection.setAutoCommit( true );
				statement.executeUpdate( "ALTER DATABASE CURRENT COLLATE " + originalDBCollation );
			}
			catch (SQLException e) {
				throw new RuntimeException( "Failed to revert back database collation to " + originalDBCollation, e );
			}
			finally {
				serviceRegistry.destroy();
			}
		}
	}

	protected void buildSessionFactory() {
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		StandardServiceRegistryImpl serviceRegistry = buildServiceRegistry( bootRegistry, constructConfiguration() );

		try {
			try ( Connection connection = serviceRegistry.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess().obtainConnection();
				 Statement statement = connection.createStatement() ) {
				connection.setAutoCommit( true );
				try ( ResultSet rs = statement.executeQuery( "SELECT SERVERPROPERTY('collation')" ) ) {
					rs.next();
					String instanceCollation = rs.getString( 1 );
					Assert.assertNotEquals( instanceCollation, changedDBCollation );
				}
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			try ( Connection connection = serviceRegistry.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess().obtainConnection();
				  Statement statement = connection.createStatement() ) {
				connection.setAutoCommit( true );
				try ( ResultSet rs = statement.executeQuery( "SELECT CONVERT (varchar(256), DATABASEPROPERTYEX(DB_NAME(),'collation'))" ) ) {
					rs.next();
					originalDBCollation = rs.getString( 1 );
				}
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			try ( Connection connection = serviceRegistry.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess().obtainConnection();
				 Statement statement = connection.createStatement() ) {
				connection.setAutoCommit( true );
				statement.executeUpdate( "ALTER DATABASE CURRENT COLLATE " + changedDBCollation );
				collationChanged = true;
			}
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
		finally {
			serviceRegistry.destroy();
		}
		super.buildSessionFactory();
	}

	@Test
	public void testTemporaryTableCreateWithoutCollationConflict() {
		// without fixing "HHH-3326", the following exception will be thrown:
		// Cannot resolve the collation conflict between "SQL_Latin1_General_CP1_CI_AS" and "SQL_Latin1_General_CP437_BIN" in the equal to operation.
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "update Woman w set w.description = :description where w.age > :age" )
					.setParameter( "description", "your are old" )
					.setParameter( "age", 30 )
					.executeUpdate();
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Human.class,
				Woman.class
		};
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Entity(name = "Human")
	@Table(name = "Human")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Human {
		@Id
		String id;

		int age;
	}

	@Entity(name = "Woman")
	@Table(name = "Woman")
	public static class Woman extends Human {
		String description;
	}
}
