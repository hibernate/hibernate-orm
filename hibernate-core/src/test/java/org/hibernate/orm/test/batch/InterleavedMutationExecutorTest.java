package org.hibernate.orm.test.batch;

import java.util.List;
import org.hibernate.JDBCException;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.exception.ConstraintViolationException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationExecutorSingleBatched;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;
import org.hibernate.sql.spi.mutation.jdbc.JdbcUpdateMutation;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Uses real JDBC batches and a real coordinator to exercise reused single-table executors.
/// Current update, history closure and replacement insert deliberately alternate batch keys.
///
/// @author Steve Ebersole
@JiraKey("HHH-21102")
@DomainModel(annotatedClasses = InterleavedMutationExecutorTest.Row.class)
@SessionFactory
class InterleavedMutationExecutorTest {
	@ParameterizedTest
	@ValueSource(ints = {2, 5})
	void alternatingKeysRetainEveryMutation(int size, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			seed( session, 3 );
			var update = executor( session, "current", size );
			var end = executor( session, "end", size );
			var insert = executor( session, "insert", size );
			try {
				var firstStatement = update.getPreparedStatementDetails( "lb_row" ).resolveStatement();
				for ( int id = 1; id <= 3; id++ ) {
					apply( update, id, session );
					apply( end, id, session );
					apply( insert, id + 10, session );
				}
				session.getJdbcCoordinator().executeBatch();
				session.doWork( connection -> assertThat( firstStatement.isClosed() ).isTrue() );
				assertRows( session, 3, true );
				assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
			}
			finally { update.release(); end.release(); insert.release(); }
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 5})
	void sameKeyBatchesAtBoundaryAndFlushesTail(int size, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			seed( session, size + 1 );
			var executions = new AtomicInteger();
			session.addEventListeners( new SessionEventListener() {
				@Override public void jdbcExecuteBatchStart() { executions.incrementAndGet(); }
			} );
			var update = executor( session, "current", size );
			try {
				var details = update.getPreparedStatementDetails( "lb_row" );
				for ( int id = 1; id <= size + 1; id++ ) {
					apply( update, id, session );
					assertThat( update.getPreparedStatementDetails( "lb_row" ) ).isSameAs( details );
					assertThat( executions ).hasValue( id < size ? 0 : 1 );
				}
				session.getJdbcCoordinator().executeBatch();
				assertThat( executions ).hasValue( 2 );
				session.doWork( connection -> {
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
							"select count(*) from lb_row where counter=1 and closed=0" ) ) {
						rows.next(); assertThat( rows.getInt( 1 ) ).isEqualTo( size + 1 );
					}
				} );
			}
			finally { update.release(); }
		} );
	}

	@Test
	void failureRollsBackExecutedAndPendingWork(SessionFactoryScope scope) {
		scope.inTransaction( session -> seed( session, 3 ) );
		try ( var session = scope.getSessionFactory().openSession() ) {
			var tx = session.beginTransaction();
			var update = executor( session, "current", 5 );
			var end = executor( session, "end", 5 );
			var insert = executor( session, "insert", 5 );
			try {
				apply( update, 1, session ); apply( end, 1, session ); apply( insert, 11, session );
				apply( update, 2, session ); apply( end, 2, session ); apply( insert, 1, session );
				assertThatThrownBy( () -> session.getJdbcCoordinator().executeBatch() )
						.satisfies( failure -> {
							if ( session.getJdbcServices().getDialect() instanceof SybaseDialect ) {
								assertThat( failure ).isInstanceOf( JDBCException.class );
								var sql = ((JDBCException) failure).getSQLException();
								assertThat( sql.getSQLState() ).isEqualTo( "JZ0BE" );
								assertThat( sql.getMessage() ).contains( "Attempt to insert duplicate key row", "lb_row" );
							}
							else assertThat( failure ).isInstanceOf( ConstraintViolationException.class );
						} );
			}
			finally { tx.rollback(); update.release(); end.release(); insert.release(); }
			assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
		}
		scope.inTransaction( session -> assertRows( session, 3, false ) );
	}

	static MutationExecutorSingleBatched executor(SessionImplementor session, String kind, int size) {
		var persister = (AbstractEntityPersister) session.getFactory().getMappingMetamodel().getEntityDescriptor( Row.class );
		var table = persister.getIdentifierTableMapping();
		var integer = session.getFactory().getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );
		var usage = kind.equals( "insert" ) ? ParameterUsage.SET : ParameterUsage.RESTRICT;
		var parameter = new ColumnValueParameter( new ColumnReference( (String) null, "id", false, null, integer ), usage );
		PreparableMutationOperation operation = kind.equals( "insert" )
				? new JdbcInsertMutation( table, persister, "insert into lb_row (id,counter,closed) values (?,1,0)", false, new Expectation.RowCount(), List.of( parameter ) )
				: new JdbcUpdateMutation( table, persister, kind.equals( "current" )
						? "update lb_row set counter=counter+1 where id=?"
						: "update lb_row set closed=1 where id=? and closed=0", false, new Expectation.RowCount(), List.of( parameter ) );
		return new MutationExecutorSingleBatched( operation, new BasicBatchKey( kind ), size, session );
	}

	static void apply(MutationExecutorSingleBatched executor, int id, SessionImplementor session) {
		// Access the statement group before binding, as callers needing statement details do.
		var details = executor.getPreparedStatementDetails( "lb_row" );
		executor.getJdbcValueBindings().bindValue( id, "lb_row", "id",
				details.getSqlString().startsWith( "insert" ) ? ParameterUsage.SET : ParameterUsage.RESTRICT );
		executor.execute( null, null, null, null, session );
	}

	static void seed(SessionImplementor session, int count) {
		session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.executeUpdate( "delete from lb_row where 1=1" );
				for ( int id = 1; id <= count; id++ ) statement.executeUpdate(
						"insert into lb_row (id,counter,closed) values (" + id + ",0,0)" );
			}
		} );
	}

	static void assertRows(SessionImplementor session, int count, boolean changed) {
		session.doWork( connection -> {
			try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
					"select id,counter,closed from lb_row order by id" ) ) {
				for ( int id = 1; id <= count; id++ ) {
					assertThat( rows.next() ).isTrue(); assertThat( rows.getInt( 1 ) ).isEqualTo( id );
					assertThat( rows.getInt( 2 ) ).isEqualTo( changed ? 1 : 0 );
					assertThat( rows.getInt( 3 ) ).isEqualTo( changed ? 1 : 0 );
				}
				if ( changed ) for ( int id = 1; id <= count; id++ ) {
					assertThat( rows.next() ).isTrue(); assertThat( rows.getInt( 1 ) ).isEqualTo( id + 10 );
					assertThat( rows.getInt( 2 ) ).isEqualTo( 1 ); assertThat( rows.getInt( 3 ) ).isZero();
				}
				assertThat( rows.next() ).isFalse();
			}
		} );
	}

	@Entity(name = "BatchLifetimeRow") @Table(name = "lb_row")
	static class Row { @Id int id; int counter; int closed; }
}
