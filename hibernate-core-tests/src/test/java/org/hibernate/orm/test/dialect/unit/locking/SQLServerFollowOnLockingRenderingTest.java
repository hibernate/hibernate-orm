package org.hibernate.orm.test.dialect.unit.locking;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the final SQL Server locking strategy and table hints without a
/// database connection or a bootstrapped SessionFactory.
///
/// @author Steve Ebersole
@Jira("https://hibernate.atlassian.net/browse/HHH-21028")
class SQLServerFollowOnLockingRenderingTest {
	@ParameterizedTest
	@EnumSource(value = Locking.FollowOn.class, names = { "ALLOW", "FORCE" })
	void keepsDirectTableHintsEvenWhenFollowOnIsForced(Locking.FollowOn strategy) {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final var types = new TypeConfiguration();
		when( factory.getTypeConfiguration() ).thenReturn( types );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new SQLServerDialect() );
		when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );
		final var querySpec = new QuerySpec( true );
		final var path = new NavigablePath( "WorkItem" );
		querySpec.applyRootPathForLocking( path );
		querySpec.getFromClause().addRoot( new StandardTableGroup(
				true, path, mock( TableGroupProducer.class ), "w", new NamedTableReference( "work_item", "w" ),
				new SqlAliasBaseConstant( "w" ), factory ) );
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0,
				new QueryLiteral<>( 1, types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) ) ) );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE )
				.setTimeout( Timeouts.SKIP_LOCKED ).setFollowOnStrategy( strategy );
		final var operation = new SQLServerSqlAstTranslator<>(
				new SqlAstTranslationRequest.Select( factory, new SelectStatement( querySpec ) ) )
				.translate( null, options );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getSqlString() ).contains( "updlock", "readpast" );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}
}
