/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Restricted to H2 as there is nothing intrinsically Dialect specific here,
 * though each database has specific syntax for labelled parameters
 *
 * @author Steve Ebersole
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = JdbcParameterRenderer.class,
		impl = JdbcParameterRendererTests.JdbcParameterRendererImpl.class
) )
@DomainModel( annotatedClasses = { EntityOfBasics.class, JdbcParameterRendererTests.EntityWithFilters.class } )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialect( H2Dialect.class )
public class JdbcParameterRendererTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
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

		statementInspector.clear();
		scope.inTransaction( (session) -> {
			final EntityWithFilters it = new EntityWithFilters( 1, "It", "EMEA" );
			session.persist( it );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 3 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?2" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?3" );

		scope.inTransaction( (session) -> {
			final EntityWithFilters it = session.find( EntityWithFilters.class, 1 );
			statementInspector.clear();
			it.setName( "It 2" );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 3 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?2" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?3" );

		scope.inTransaction( (session) -> {
			final EntityWithFilters it = session.find( EntityWithFilters.class, 1 );
			statementInspector.clear();
			session.remove( it );
		} );
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 1 );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "?1" );
	}

	public static class JdbcParameterRendererImpl implements JdbcParameterRenderer {
		@Override
		public String renderJdbcParameter(int position, JdbcType jdbcType) {
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
}
