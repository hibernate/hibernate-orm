/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typeparameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for parameterizable types.
 *
 * @author Michael Gloegl
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/typeparameters/Typedef.hbm.xml",
				"org/hibernate/orm/test/typeparameters/Widget.hbm.xml"
		}
)
@SessionFactory
public class TypeParameterTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Widget" ).executeUpdate()

		);
	}

	@Test
	public void testSave(SessionFactoryScope scope) {
		final Integer id = (Integer) scope.fromTransaction(
				session -> {
					Widget obj = new Widget();
					obj.setValueThree( 5 );
					return session.save( obj );
				}
		);

		scope.inSession(
				session ->
						doWork( id, session )
		);
	}

	private void doWork(final Integer id, final Session s) {
		s.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						final String sql = "SELECT * FROM STRANGE_TYPED_OBJECT WHERE id=?";
						PreparedStatement statement = ( (SessionImplementor) s ).getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql );
						statement.setInt( 1, id.intValue() );
						ResultSet resultSet = ( (SessionImplementor) s ).getJdbcCoordinator()
								.getResultSetReturn()
								.extract( statement, sql );

						assertTrue( "A row should have been returned", resultSet.next() );
						assertTrue(
								"Default value should have been mapped to null",
								resultSet.getObject( "value_one" ) == null
						);
						assertTrue(
								"Default value should have been mapped to null",
								resultSet.getObject( "value_two" ) == null
						);
						assertEquals( "Non-Default value should not be changed", resultSet.getInt( "value_three" ), 5 );
						assertTrue(
								"Default value should have been mapped to null",
								resultSet.getObject( "value_four" ) == null
						);
					}
				}
		);
	}

	@Test
	public void testLoading(SessionFactoryScope scope) throws Exception {
		initData( scope );
		scope.inTransaction(
				session -> {
					Widget obj = (Widget) session.createQuery( "from Widget o where o.string = :string" ).setParameter(
							"string",
							"all-normal"
					).uniqueResult();
					assertEquals( "Non-Default value incorrectly loaded", obj.getValueOne(), 7 );
					assertEquals( "Non-Default value incorrectly loaded", obj.getValueTwo(), 8 );
					assertEquals( "Non-Default value incorrectly loaded", obj.getValueThree(), 9 );
					assertEquals( "Non-Default value incorrectly loaded", obj.getValueFour(), 10 );

					obj = (Widget) session.createQuery( "from Widget o where o.string = :string" )
							.setParameter( "string", "all-default" )
							.uniqueResult();
					assertEquals( "Default value incorrectly loaded", obj.getValueOne(), 1 );
					assertEquals( "Default value incorrectly loaded", obj.getValueTwo(), 2 );
					assertEquals( "Default value incorrectly loaded", obj.getValueThree(), -1 );
					assertEquals( "Default value incorrectly loaded", obj.getValueFour(), -5 );

				}
		);
	}

	public void initData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Widget obj = new Widget();
					obj.setValueOne( 7 );
					obj.setValueTwo( 8 );
					obj.setValueThree( 9 );
					obj.setValueFour( 10 );
					obj.setString( "all-normal" );
					session.save( obj );

					obj = new Widget();
					obj.setValueOne( 1 );
					obj.setValueTwo( 2 );
					obj.setValueThree( -1 );
					obj.setValueFour( -5 );
					obj.setString( "all-default" );
					session.save( obj );
				}
		);
	}
}
