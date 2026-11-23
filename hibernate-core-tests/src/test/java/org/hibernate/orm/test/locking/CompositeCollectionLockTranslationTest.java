package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.lock.EntityDetails;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Translates the real collection acquisition entry points with SQL Server's
/// dialect and captures JDBC operations before execution. This does not simulate
/// or validate SQL Server's database locking behavior.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = CompositeCollectionLockingTest.Owner.class)
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = @Setting(name = "hibernate.use_sql_comments", value = "false"))
class CompositeCollectionLockTranslationTest {
	@ParameterizedTest
	@CsvSource({
			"false,false,0", "false,false,1", "false,false,2", "false,false,3",
			"false,true,0", "false,true,1", "false,true,2", "false,true,3",
			"true,false,0", "true,false,1", "true,false,2", "true,false,3",
			"true,true,0", "true,true,1", "true,true,2", "true,true,3"
	})
	void translatesCompleteKeysWithMatchingBindingsAndTableHints(boolean collected, boolean propertyRef, int count, SessionFactoryScope scope) {
		scope.inSession( realSession -> {
			final var persister = realSession.getFactory().getMappingMetamodel().getEntityDescriptor( CompositeCollectionLockingTest.Owner.class );
			final var entries = new IdentityHashMap<Object, EntityEntry>();
			final var owners = new ArrayList<CompositeCollectionLockingTest.Owner>();
			for ( int i = 0; i < Math.min( count, 2 ); i++ ) {
				final var owner = new CompositeCollectionLockingTest.Owner( CompositeCollectionLockingTest.SELECTED.get( i ) );
				final var entry = mock( EntityEntry.class );
				when( entry.getId() ).thenReturn( owner.id );
				when( entry.getPersister() ).thenReturn( persister );
				when( entry.getLoadedState() ).thenReturn( new Object[1] );
				when( entry.getLoadedValue( anyString() ) ).thenReturn( owner.business );
				entries.put( owner, entry );
				owners.add( owner );
			}
			if ( count == 3 ) {
				owners.add( owners.get( 0 ) );
			}
			final var captured = new AtomicReference<Acquisition>();
			final var session = capturingSession( realSession, entries, captured );
			final var attribute = CompositeCollectionLockingTest.attribute( session, propertyRef );
			final var context = new BaseExecutionContext( session );
			if ( collected ) {
				final var keys = owners.stream().map( owner -> new CollectionKey( attribute.getCollectionDescriptor(),
						propertyRef ? owner.business : owner.id ) ).toList();
				LockingHelper.lockCollectionTable( attribute, PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER, keys, context );
			}
			else {
				final Map<Object, EntityDetails> details = new LinkedHashMap<>();
				owners.forEach( owner -> details.put( owner.id,
						new EntityDetails( session.generateEntityKey( owner.id, persister ), entries.get( owner ), owner ) ) );
				LockingHelper.lockCollectionTable( attribute, PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER, details, context );
			}
			final var operation = captured.get().operation;
			final var bindings = captured.get().bindings;
			assertThat( operation.getLockingApplication() ).isNotEqualTo( JdbcLockingApplication.FOLLOW_ON );
			assertThat( operation.getSqlString() ).contains( "with (updlock,holdlock,rowlock)" ).doesNotContain( " in (", " for update" );
			final var input = collected ? owners : owners.subList( 0, Math.min( count, 2 ) );
			final List<Object> expected = new ArrayList<>();
			final List<String> columns = new ArrayList<>();
			attribute.getKeyDescriptor().forEachSelectable( (i, selectable) -> columns.add( selectable.getSelectionExpression() ) );
			for ( var owner : input ) {
				for ( var column : columns ) {
					expected.add( switch ( column ) {
						case "local_fk" -> owner.id.local;
						case "tenant_fk" -> owner.id.tenant;
						case "business_number_fk" -> owner.business.number;
						case "business_region_fk" -> owner.business.region;
						default -> throw new AssertionError( "Unexpected physical key column: " + column );
					} );
				}
			}
			final var actual = operation.getParameterBinders().stream().map( binder -> {
				final var binding = bindings.getBinding( (JdbcParameter) binder );
				assertThat( binding.getBindType().getJdbcJavaType().getJavaTypeClass() ).isEqualTo( Integer.class );
				return binding.getBindValue();
			} ).toList();
			assertThat( actual ).containsExactlyElementsOf( expected );
			assertThat( bindings.getBindings() ).hasSize( expected.size() );
			if ( count == 0 ) {
				assertThat( operation.getSqlString() ).contains( "1=0" );
			}
			else {
				assertThat( operation.getSqlString() ).contains( "tbl." + columns.get( 0 ) + "=?", "tbl." + columns.get( 1 ) + "=?" );
				if ( input.size() > 1 ) {
					assertThat( operation.getSqlString() ).contains( " or " );
				}
			}
		} );
	}

	private static SessionImplementor capturingSession(
			SessionImplementor realSession, Map<Object, EntityEntry> entries, AtomicReference<Acquisition> captured) {
		final var executor = mock( JdbcSelectExecutor.class );
		doAnswer( call -> {
			captured.set( new Acquisition( call.getArgument( 0 ), call.getArgument( 1 ) ) );
			return List.of();
		} ).when( executor ).executeQuery( any(), any(), any(), any(), any(), any(), any() );
		final var services = mock( JdbcServices.class, delegatesTo( realSession.getJdbcServices() ) );
		doReturn( new SQLServerDialect( DatabaseVersion.make( 16 ) ) ).when( services ).getDialect();
		doReturn( executor ).when( services ).getJdbcSelectExecutor();
		final var factory = mock( SessionFactoryImplementor.class, delegatesTo( realSession.getFactory() ) );
		doReturn( services ).when( factory ).getJdbcServices();
		final var persistenceContext = mock( PersistenceContext.class, delegatesTo( realSession.getPersistenceContextInternal() ) );
		doAnswer( call -> entries.get( call.getArgument( 0 ) ) ).when( persistenceContext ).getEntry( any() );
		final var session = mock( SessionImplementor.class, delegatesTo( realSession ) );
		doReturn( factory ).when( session ).getFactory();
		doReturn( factory ).when( session ).getSessionFactory();
		doReturn( services.getDialect() ).when( session ).getDialect();
		doReturn( persistenceContext ).when( session ).getPersistenceContextInternal();
		return session;
	}

	private record Acquisition(JdbcSelect operation, JdbcParameterBindings bindings) {}
}
