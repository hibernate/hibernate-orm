/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.autodiscovery;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Group.class,
				User.class,
				Membership.class
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				provider = AutoDiscoveryTest.ImplicitNamingStrategyProvider.class
		)
)
public class AutoDiscoveryTest {
	private static final String QUERY_STRING =
			"select u.name as username, g.name as groupname, m.joinDate " +
			"from t_membership m " +
			"        inner join t_user u on m.member_id = u.id " +
			"        inner join t_group g on m.group_id = g.id";

	public static class ImplicitNamingStrategyProvider implements SettingProvider.Provider<ImplicitNamingStrategy> {
		@Override
		public ImplicitNamingStrategy getSetting() {
			return ImplicitNamingStrategyJpaCompliantImpl.INSTANCE;
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testAutoDiscoveryWithDuplicateColumnLabels(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new User( "steve" ) );
					session.persist( new User( "stliu" ) );
				}
		);

		scope.inTransaction(
				session -> {
					List<Object[]> results = session.createNativeQuery(
									"select u.name, u2.name from t_user u, t_user u2 where u.name='steve'", Object[].class )
							.list();
					// this should result in a result set like:
					//   [0] steve, steve
					//   [1] steve, stliu
					// although the rows could be reversed
					assertThat( results ).hasSize( 2 );
					final Object[] row1 = results.get( 0 );
					final Object[] row2 = results.get( 1 );
					assertThat( row1[0] ).isEqualTo( "steve" );
					assertThat( row2[0] ).isEqualTo( "steve" );
					if ( "steve".equals( row1[1] ) ) {
						assertThat( row2[1] ).isEqualTo( "stliu" );
					}
					else {
						assertThat( row1[1] ).isEqualTo( "stliu" );
					}

				}
		);
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from User" ).executeUpdate()
		);
	}

	@Test
	public void testSqlQueryAutoDiscovery(SessionFactoryScope scope) {
		User u = new User( "steve" );
		Group g = new Group( "developer" );
		Membership m = new Membership( u, g );
		scope.inTransaction(
				session -> {
					session.persist( u );
					session.persist( g );
					session.persist( m );
				}
		);

		scope.inTransaction(
				session -> {
					List<Object[]> result = session.createNativeQuery( QUERY_STRING, Object[].class ).list();
					Object[] row = result.get( 0 );
					assertThat( row[0] ).isEqualTo( "steve" );
					assertThat( row[1] ).isEqualTo( "developer" );
					session.remove( m );
					session.remove( u );
					session.remove( g );
				}
		);
	}

	@Test
	public void testDialectGetColumnAliasExtractor(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork(
								connection -> {
									Dialect dialect = session.getDialect();
									PreparedStatement ps = session.getJdbcCoordinator()
											.getStatementPreparer().prepareStatement( QUERY_STRING );
									ResultSet rs = session.getJdbcCoordinator().getResultSetReturn()
											.extract( ps, QUERY_STRING );
									try {
										ResultSetMetaData metadata = rs.getMetaData();
										String column1Alias = dialect.getColumnAliasExtractor()
												.extractColumnAlias( metadata, 1 );
										String column2Alias = dialect.getColumnAliasExtractor()
												.extractColumnAlias( metadata, 2 );
										assertThat( column1Alias.equals( column2Alias ) )
												.describedAs( "bad dialect.getColumnAliasExtractor impl" )
												.isFalse();
									}
									finally {
										session.getJdbcCoordinator().getLogicalConnection()
												.getResourceRegistry().release( rs, ps );
										session.getJdbcCoordinator().getLogicalConnection()
												.getResourceRegistry().release( ps );
									}
								}
						)
		);
	}

	@Test
	@JiraKey("HHH-16697")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase sum(39.74) returns Float",
			matchSubTypes = true)
	public void testAggregateQueryAutoDiscovery(SessionFactoryScope scope) {
		User u = new User( "steve" );
		scope.inTransaction(
				session -> session.persist( u )
		);

		scope.inTransaction(
				session -> {
					List<Object> result = session.createNativeQuery( "select sum(39.74) from t_user u", Object.class )
							.list();
					assertThat( result.get( 0 ) ).isEqualTo( new BigDecimal( "39.74" ) );
					session.remove( u );
				}
		);
	}
}
