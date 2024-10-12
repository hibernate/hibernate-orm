/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast;

import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.internal.util.StringHelper.*;

/**
 * @implNote Restricted to H2 as there is nothing intrinsically Dialect specific here,
 * though each database has specific syntax for labelled parameters
 *
 * @author Steve Ebersole
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = ParameterMarkerStrategy.class,
		impl = ParameterMarkerStrategyTests.ParameterMarkerStrategyImpl.class )
)
@DomainModel( annotatedClasses = {
		EntityOfBasics.class,
		ParameterMarkerStrategyTests.EntityWithFilters.class,
		ParameterMarkerStrategyTests.EntityWithVersion.class
} )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialect( H2Dialect.class )
public class ParameterMarkerStrategyTests {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16229" )
	public void testQueryParams(SessionFactoryScope scope) {
		final String queryString = "select e from EntityOfBasics e where e.id = :id";

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.createSelectionQuery( queryString, EntityOfBasics.class ).setParameter( "id", 1 ).list();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		final String sql = statementInspector.getSqlQueries().get( 0 );
		assertThat( sql ).contains( "?1" );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16260" )
	public void testFilters(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.enableFilter( "region" ).setParameter( "region", "NA" );
			session.createSelectionQuery( "from EntityWithFilters", EntityWithFilters.class ).list();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			final String sql = statementInspector.getSqlQueries().get( 0 );
			assertThat( sql ).contains( "?1" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16256" )
	public void testMutations(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final EntityWithFilters it = new EntityWithFilters( 1, "It", "EMEA" );
			session.persist( it );
			session.flush();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 3 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?2" );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?3" );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithFilters it = session.find( EntityWithFilters.class, 1 );
			statementInspector.clear();
			it.setName( "It 2" );
			session.flush();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 3 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?2" );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?3" );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithFilters it = session.find( EntityWithFilters.class, 1 );
			statementInspector.clear();
			session.remove( it );
			session.flush();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16283" )
	public void testNativeQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		statementInspector.clear();
		scope.inTransaction( (session) -> {
			session.createNativeQuery( "select count(1) from filtered_entity e where e.region = :region" )
					.setParameter( "region", "ABC" )
					.uniqueResult();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
		} );

		statementInspector.clear();
		scope.inTransaction( (session) -> {
			session.createNativeQuery( "select count(1) from filtered_entity e where e.region in (:region)" )
					.setParameterList( "region", List.of( "ABC", "DEF" ) )
					.uniqueResult();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1", "?2" );
		} );
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete EntityOfBasics" ).executeUpdate();
			session.createMutationQuery( "delete EntityWithFilters" ).executeUpdate();
			session.createMutationQuery( "delete EntityWithVersion" ).executeUpdate();
		} );
	}

	public static class ParameterMarkerStrategyImpl implements ParameterMarkerStrategy {
		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return "?" + position;
		}
	}

	@Entity( name = "EntityWithFilters" )
	@Table( name = "filtered_entity" )
	@FilterDef(
			name = "region",
			defaultCondition = "region = :region",
			parameters = @ParamDef(name = "region", type = String.class)
	)
	@Filter( name = "region" )
	public static class EntityWithFilters {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Basic
		private String region;

		protected EntityWithFilters() {
			// for use by Hibernate
		}

		public EntityWithFilters(Integer id, String name, String region) {
			this.id = id;
			this.name = name;
			this.region = region;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}
	}

	@Entity( name = "EntityWithVersion" )
	@Table( name = "versioned_entity" )
	public static class EntityWithVersion {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Version
		private int version;

		private EntityWithVersion() {
			// for use by Hibernate
		}

		public EntityWithVersion(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
