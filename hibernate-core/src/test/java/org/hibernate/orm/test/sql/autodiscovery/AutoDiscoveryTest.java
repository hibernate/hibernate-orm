/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.autodiscovery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { Group.class, User.class, Membership.class }
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa")
)
public class AutoDiscoveryTest {
	private static final String QUERY_STRING =
			"select u.name as username, g.name as groupname, m.joinDate " +
					"from t_membership m " +
					"        inner join t_user u on m.member_id = u.id " +
					"        inner join t_group g on m.group_id = g.id";


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Test
	public void testAutoDiscoveryWithDuplicateColumnLabels(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.save( new User( "steve" ) );
					session.save( new User( "stliu" ) );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createNativeQuery(
							"select u.name, u2.name from t_user u, t_user u2 where u.name='steve'" ).list();
					// this should result in a result set like:
					//   [0] steve, steve
					//   [1] steve, stliu
					// although the rows could be reversed
					assertEquals( 2, results.size() );
					final Object[] row1 = (Object[]) results.get( 0 );
					final Object[] row2 = (Object[]) results.get( 1 );
					assertEquals( "steve", row1[0] );
					assertEquals( "steve", row2[0] );
					if ( "steve".equals( row1[1] ) ) {
						assertEquals( "stliu", row2[1] );
					}
					else {
						assertEquals( "stliu", row1[1] );
					}
				}
		);
	}

	@Test
	public void testSqlQueryAutoDiscovery(SessionFactoryScope scope) {
		User u = new User( "steve" );
		Group g = new Group( "developer" );
		Membership m = new Membership( u, g );

		scope.inTransaction(
				session -> {
					session.save( u );
					session.save( g );
					session.save( m );
				}
		);

		scope.inTransaction(
				session -> {
					List result = session.createNativeQuery( QUERY_STRING ).list();
					Object[] row = (Object[]) result.get( 0 );
					assertEquals( "steve", row[0] );
					assertEquals( "developer", row[1] );
					session.delete( m );
					session.delete( u );
					session.delete( g );
				}
		);
	}

	@Test
	public void testDialectGetColumnAliasExtractor(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.doWork(
							new Work() {
								@Override
								public void execute(Connection connection) throws SQLException {
									PreparedStatement ps = session.getJdbcCoordinator()
											.getStatementPreparer()
											.prepareStatement( QUERY_STRING );
									ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract(
											ps );
									try {
										ResultSetMetaData metadata = rs.getMetaData();
										String column1Alias = session.getFactory()
												.getJdbcServices()
												.getDialect()
												.getColumnAliasExtractor()
												.extractColumnAlias(
														metadata,
														1
												);
										String column2Alias = session.getFactory()
												.getJdbcServices()
												.getDialect()
												.getColumnAliasExtractor()
												.extractColumnAlias(
														metadata,
														2
												);
										assertFalse(
												column1Alias.equals( column2Alias ),
												"bad dialect.getColumnAliasExtractor impl"
										);
									}
									finally {
										session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, ps );
										session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
									}
								}
							}
					);
				}
		);
	}
}
