package org.hibernate.orm.test.resource;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.resource.jdbc.internal.ResourceRegistryStandardImpl;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Release-counter boundaries, including cleanup, failures, and non-prepared statements.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-20993")
class StatementReleaseStatisticsTests {
	@Test
	void preparationEndDoesNotRecordReleaseEvenAfterFailedPreparation() {
		var statistics = statistics();
		var handler = new JdbcEventHandler( statistics, null, null );
		handler.jdbcPrepareStatementStart();
		// Called from a finally block even when preparing the JDBC statement throws.
		handler.jdbcPrepareStatementEnd();
		verify( statistics ).prepareStatement();
		verify( statistics, never() ).closeStatement();
	}

	@ParameterizedTest
	@ValueSource(classes = { PreparedStatement.class, CallableStatement.class })
	void successfulReleaseIsCountedAfterClose(Class<? extends Statement> statementType) throws Exception {
		var statistics = statistics();
		var statement = mock( statementType );
		var registry = registry( statistics );
		registry.register( statement, false );
		registry.release( statement );
		var order = inOrder( statement, statistics );
		order.verify( statement ).close();
		order.verify( statistics ).closeStatement();
	}

	@Test
	void resourceCleanupCountsEachPreparedStatementOnce() throws Exception {
		var statistics = statistics();
		var first = mock( PreparedStatement.class );
		var second = mock( PreparedStatement.class );
		var registry = registry( statistics );
		registry.register( first, false );
		registry.register( second, false );
		registry.releaseResources();
		registry.releaseResources();
		verify( first ).close();
		verify( second ).close();
		verify( statistics, times( 2 ) ).closeStatement();
	}

	@Test
	void plainStatementReleaseDoesNotIncrementPreparedStatementCounter() throws Exception {
		var statistics = statistics();
		var statement = mock( Statement.class );
		var registry = registry( statistics );
		registry.register( statement, false );
		registry.release( statement );
		verify( statement ).close();
		verify( statistics, never() ).closeStatement();
	}

	@Test
	void failedCloseIsNotCounted() throws Exception {
		var statistics = statistics();
		var statement = mock( PreparedStatement.class );
		doThrow( new SQLException( "close failed" ) ).when( statement ).close();
		var registry = registry( statistics );
		registry.register( statement, false );
		registry.release( statement );
		verify( statement ).close();
		verify( statistics, never() ).closeStatement();
	}

	@Test
	void failureBeforeCloseIsNotCounted() throws Exception {
		var statistics = statistics();
		var statement = mock( PreparedStatement.class );
		when( statement.getMaxRows() ).thenThrow( new SQLException( "cleanup failed" ) );
		var registry = registry( statistics );
		registry.register( statement, false );
		registry.release( statement );
		verify( statement, never() ).close();
		verify( statistics, never() ).closeStatement();
	}

	@Test
	void disabledStatisticsStillClosesTheStatement() throws Exception {
		var statistics = statistics();
		when( statistics.isStatisticsEnabled() ).thenReturn( false );
		var statement = mock( PreparedStatement.class );
		var registry = registry( statistics );
		registry.register( statement, false );
		registry.release( statement );
		verify( statement ).close();
		verify( statistics, never() ).closeStatement();
	}

	@Test
	void registryWithoutEventHandlerStillClosesTheStatement() throws Exception {
		var statement = mock( PreparedStatement.class );
		var registry = new ResourceRegistryStandardImpl();
		registry.register( statement, false );
		registry.release( statement );
		verify( statement ).close();
	}

	private static StatisticsImplementor statistics() {
		var statistics = mock( StatisticsImplementor.class );
		when( statistics.isStatisticsEnabled() ).thenReturn( true );
		return statistics;
	}

	private static ResourceRegistryStandardImpl registry(StatisticsImplementor statistics) {
		return new ResourceRegistryStandardImpl( new JdbcEventHandler( statistics, null, null ) );
	}
}
