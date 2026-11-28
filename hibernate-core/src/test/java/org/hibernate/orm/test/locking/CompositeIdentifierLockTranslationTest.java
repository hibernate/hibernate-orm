package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.LockMode;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.lock.TableLock;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/// Translates entity acquisition with the production SQL Server dialect without
/// executing its SQL against the database used to bootstrap the mapping.
@DomainModel(annotatedClasses = CompositeIdentifierFollowOnLockingTest.EmbeddedItem.class)
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = @Setting(name = "hibernate.use_sql_comments", value = "false"))
class CompositeIdentifierLockTranslationTest {
	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2, 3 })
	void emulatesCompleteIdentifiersWithMatchingBindings(int count, SessionFactoryScope scope) {
		scope.inSession( realSession -> {
			final var session = sqlServerSession( realSession );
			final var persister = session.getFactory().getMappingMetamodel()
					.getEntityDescriptor( CompositeIdentifierFollowOnLockingTest.EmbeddedItem.class );
			final var table = new AtomicReference<TableDetails>();
			persister.forEachTableDetails( details -> { if ( details.isIdentifierTable() ) { table.set( details ); } } );
			final var identifiers = List.of(
					new CompositeIdentifierFollowOnLockingTest.Pair( 1, 2 ),
					new CompositeIdentifierFollowOnLockingTest.Pair( 1, 3 ),
					new CompositeIdentifierFollowOnLockingTest.Pair( 1, 2 ) ).subList( 0, count );
			final var keys = identifiers.stream().map( id -> session.generateEntityKey( id, persister ) ).toList();
			final var lock = new ExposedTableLock( table.get(), persister, keys, session );
			final var operation = lock.translate( session );
			assertThat( operation.getLockingApplication() ).isNotEqualTo( JdbcLockingApplication.FOLLOW_ON );
			assertThat( operation.getSqlString() ).contains( "with (updlock,holdlock,rowlock)" )
					.doesNotContain( " in (", " for update" );
			final var expected = new ArrayList<Integer>();
			for ( var id : identifiers ) {
				table.get().getKeyDetails().forEachKeyColumn( (position, column) ->
						expected.add( switch ( column.getColumnName() ) {
							case "local_key" -> id.local;
							case "tenant_key" -> id.tenant;
							default -> throw new AssertionError( column.getColumnName() );
						} ) );
			}
			assertThat( lock.boundValues( operation ) ).containsExactlyElementsOf( expected );
			if ( count == 0 ) {
				assertThat( operation.getSqlString() ).contains( "1=0" );
			}
			else {
				assertThat( operation.getSqlString() ).contains( "tbl.local_key=?", "tbl.tenant_key=?" );
				if ( count > 1 ) {
					assertThat( operation.getSqlString() ).contains( " or " );
				}
			}
		} );
	}

	private static SessionImplementor sqlServerSession(SessionImplementor realSession) {
		final var services = mock( JdbcServices.class, delegatesTo( realSession.getJdbcServices() ) );
		doReturn( new SQLServerDialect( DatabaseVersion.make( 16 ) ) ).when( services ).getDialect();
		final var factory = mock( SessionFactoryImplementor.class, delegatesTo( realSession.getFactory() ) );
		doReturn( services ).when( factory ).getJdbcServices();
		final var session = mock( SessionImplementor.class, delegatesTo( realSession ) );
		doReturn( factory ).when( session ).getFactory();
		doReturn( factory ).when( session ).getSessionFactory();
		doReturn( services.getDialect() ).when( session ).getDialect();
		return session;
	}

	private static class ExposedTableLock extends TableLock {
		ExposedTableLock(TableDetails table, EntityMappingType mapping, List<EntityKey> keys, SessionImplementor session) {
			super( table, mapping, keys, session );
		}

		JdbcSelect translate(SessionImplementor session) {
			final var options = new QueryOptionsImpl();
			options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
			return session.getDialect().getSqlAstTranslatorFactory().buildTranslator(
					new SqlAstTranslationRequest.Select( session.getFactory(), new SelectStatement( querySpec, domainResults ) ) )
					.translate( jdbcParameterBindings, options );
		}

		List<Object> boundValues(JdbcSelect operation) {
			assertThat( jdbcParameterBindings.getBindings() ).hasSize( operation.getParameterBinders().size() );
			return operation.getParameterBinders().stream().map( binder -> {
				final var binding = jdbcParameterBindings.getBinding( (JdbcParameter) binder );
				assertThat( binding.getBindType().getJdbcJavaType().getJavaTypeClass() ).isEqualTo( Integer.class );
				return binding.getBindValue();
			} ).toList();
		}
	}
}
