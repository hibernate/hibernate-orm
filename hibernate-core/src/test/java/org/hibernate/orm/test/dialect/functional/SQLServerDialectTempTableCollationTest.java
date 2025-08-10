/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Nathan Xu
 */
@RequiresDialect( SQLServerDialect.class )
@JiraKey( value = "HHH-3326" )
public class SQLServerDialectTempTableCollationTest extends BaseCoreFunctionalTestCase {

	private String originalDBCollation;
	private final String changedDBCollation = "SQL_Latin1_General_CP437_BIN";
	private boolean collationChanged;

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, Boolean.TRUE.toString() );
	}

	@Override
	protected void releaseSessionFactory() {
		super.releaseSessionFactory();
		if ( originalDBCollation != null && collationChanged && !changedDBCollation.equals( originalDBCollation ) ) {
			BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
			StandardServiceRegistryImpl serviceRegistry = buildServiceRegistry(
					bootRegistry,
					constructAndConfigureConfiguration( bootRegistry )
			);
			try {
				TransactionUtil.doWithJDBC(
						serviceRegistry,
						connection -> {
							try (Statement statement = connection.createStatement()) {
								connection.setAutoCommit( true );
								String dbName;
								try ( ResultSet rs = statement.executeQuery( "SELECT DB_NAME()" ) ) {
									rs.next();
									dbName = rs.getString( 1 );
								}
								statement.execute( "USE master" );
								statement.execute( "ALTER DATABASE " + dbName + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE" );
								statement.executeUpdate( "ALTER DATABASE " + dbName + " COLLATE " + originalDBCollation );
								statement.execute( "ALTER DATABASE " + dbName + " SET MULTI_USER WITH ROLLBACK IMMEDIATE" );
								statement.execute( "USE " + dbName );
							}
						}
				);
			}
			catch (SQLException e) {
				throw new RuntimeException( "Failed to revert back database collation to " + originalDBCollation, e );
			}
			finally {
				serviceRegistry.destroy();
			}
		}
		// The alter database calls could lead to issues with existing connections, so we reset the shared pool here
		SharedDriverManagerConnectionProvider.getInstance().reset();
	}

	@Override
	protected void buildSessionFactory() {
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		StandardServiceRegistryImpl serviceRegistry =
				buildServiceRegistry( bootRegistry, constructAndConfigureConfiguration( bootRegistry ) );

		try {
			try {
				TransactionUtil.doWithJDBC(
						serviceRegistry,
						connection -> {
							try (Statement statement = connection.createStatement()) {
								connection.setAutoCommit( true );
								try ( ResultSet rs = statement.executeQuery( "SELECT DATABASEPROPERTYEX(DB_NAME(),'collation')" ) ) {
									rs.next();
									String instanceCollation = rs.getString( 1 );
									Assert.assertNotEquals( instanceCollation, changedDBCollation );
								}
							}
						}
				);
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			try {
				TransactionUtil.doWithJDBC(
						serviceRegistry,
						connection -> {
							try (Statement statement = connection.createStatement()) {
								connection.setAutoCommit( true );
								try ( ResultSet rs = statement.executeQuery( "SELECT CONVERT (varchar(256), DATABASEPROPERTYEX(DB_NAME(),'collation'))" ) ) {
									rs.next();
									originalDBCollation = rs.getString( 1 );
								}
							}
						}
				);
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			TransactionUtil.doWithJDBC(
					serviceRegistry,
					connection -> {
						try (Statement statement = connection.createStatement()) {
							connection.setAutoCommit( true );
							String dbName;
							try ( ResultSet rs = statement.executeQuery( "SELECT DB_NAME()" ) ) {
								rs.next();
								dbName = rs.getString( 1 );
							}
							statement.execute( "USE master" );
							statement.execute( "ALTER DATABASE " + dbName + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE" );
							statement.executeUpdate( "ALTER DATABASE " + dbName + " COLLATE " + changedDBCollation );
							statement.execute( "ALTER DATABASE " + dbName + " SET MULTI_USER WITH ROLLBACK IMMEDIATE" );
							statement.execute( "USE " + dbName );
							collationChanged = true;
						}
					}
			);
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
