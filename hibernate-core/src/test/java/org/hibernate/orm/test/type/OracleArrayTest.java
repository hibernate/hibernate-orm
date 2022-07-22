/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@RequiresDialect( OracleDialect.class )
@TestForIssue( jiraKey = "HHH-10999")
public class OracleArrayTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(
				Environment.DIALECT,
				MyOracleDialect.class.getName()
		);
	}

	@Override
	protected void releaseSessionFactory() {
		super.releaseSessionFactory();
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
							statement.execute( "DROP TYPE INTARRAY" );
							statement.execute( "DROP TYPE TEXTARRAY" );
						}
					}
			);
		}
		catch (SQLException e) {
			throw new RuntimeException( "Failed to drop type", e );
		}
		finally {
			serviceRegistry.destroy();
		}
	}

	@Override
	protected void buildSessionFactory() {
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		StandardServiceRegistryImpl serviceRegistry =
				buildServiceRegistry( bootRegistry, constructAndConfigureConfiguration( bootRegistry ) );

		try {
			TransactionUtil.doWithJDBC(
					serviceRegistry,
					connection -> {
						try (Statement statement = connection.createStatement()) {
							connection.setAutoCommit( true );
							if ( statement.executeQuery( "SELECT 1 FROM ALL_TYPES WHERE TYPE_NAME = 'INTARRAY'" ).next() ) {
								statement.execute( "DROP TYPE INTARRAY" );
							}
							if ( statement.executeQuery( "SELECT 1 FROM ALL_TYPES WHERE TYPE_NAME = 'TEXTARRAY'" ).next() ) {
								statement.execute( "DROP TYPE TEXTARRAY" );
							}
							statement.execute( "CREATE TYPE INTARRAY AS VARRAY(10) OF NUMBER(10,0)");
							statement.execute( "CREATE TYPE TEXTARRAY AS VARRAY(10) OF VARCHAR2(255)");
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
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			ArrayHolder expected = new ArrayHolder( 1, new Integer[] { 1, 2, 3 }, new String[] { "abc", "def" } );
			session.persist( expected );
			session.flush();
			session.clear();

			ArrayHolder arrayHolder = session.find( ArrayHolder.class, 1 );
			Assert.assertEquals( expected.getIntArray(), arrayHolder.getIntArray() );
			Assert.assertEquals( expected.getTextArray(), arrayHolder.getTextArray() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ArrayHolder.class
		};
	}

	@Entity(name = "ArrayHolder")
	public static class ArrayHolder {
		@Id
		Integer id;

		@Column(columnDefinition = "TEXTARRAY")
		String[] textArray;

		@Column(columnDefinition = "TEXTARRAY")
		String[] textArray2;

		Integer[] intArray;

		public ArrayHolder() {
		}

		public ArrayHolder(Integer id, Integer[] intArray, String[] textArray) {
			this.id = id;
			this.intArray = intArray;
			this.textArray = textArray;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer[] getIntArray() {
			return intArray;
		}

		public void setIntArray(Integer[] intArray) {
			this.intArray = intArray;
		}

		public String[] getTextArray() {
			return textArray;
		}

		public void setTextArray(String[] textArray) {
			this.textArray = textArray;
		}

		public String[] getTextArray2() {
			return textArray2;
		}

		public void setTextArray2(String[] textArray2) {
			this.textArray2 = textArray2;
		}
	}

	public static class MyOracleDialect extends OracleDialect {

		public MyOracleDialect() {
		}

		public MyOracleDialect(DatabaseVersion version) {
			super( version );
		}

		public MyOracleDialect(DialectResolutionInfo info) {
			super( info );
		}

		@Override
		public String getArrayTypeName(String elementTypeName) {
			if ( "number(10,0)".equals( elementTypeName ) ) {
				return "INTARRAY";
			}
			return super.getArrayTypeName( elementTypeName );
		}
	}
}
