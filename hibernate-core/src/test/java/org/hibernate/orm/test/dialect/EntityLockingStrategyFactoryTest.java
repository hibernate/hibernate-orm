package org.hibernate.orm.test.dialect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Version;
import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SPI;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.internal.EntityLockingStrategyRequestImpl;
import org.hibernate.dialect.lock.internal.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.internal.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.internal.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.internal.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.internal.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.internal.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.internal.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.internal.SelectLockingStrategy;
import org.hibernate.dialect.lock.internal.SqlAstBasedLockingStrategy;
import org.hibernate.dialect.lock.spi.EntityLockTarget;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyRequest;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.loader.ast.internal.LoaderHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the stock entity-locking factories and Hibernate-owned request
/// boundary without requiring a live database.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		EntityLockingStrategyFactoryTest.VersionedEntity.class,
		EntityLockingStrategyFactoryTest.UnversionedEntity.class
})
@ServiceRegistry(settingProviders = @SettingProvider(
		settingName = AvailableSettings.DIALECT,
		provider = EntityLockingStrategyFactoryTest.DialectSettingProvider.class
))
@SessionFactory
public class EntityLockingStrategyFactoryTest {
	private static final AtomicInteger FACTORY_INVOCATIONS = new AtomicInteger();
	private static final EntityLockingStrategyFactory RECORDING_FACTORY =
			request -> new RecordedStrategy();
	private static volatile EntityLockingStrategyFactory suppliedFactory = RECORDING_FACTORY;

	@Test
	void dialectReturnsStableStandardFactory() {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
		};

		assertThat( dialect.getEntityLockingStrategyFactory() )
				.isSameAs( EntityLockingStrategies.standard() )
				.isSameAs( dialect.getEntityLockingStrategyFactory() );
	}

	@Test
	void stockProfilesDelegateToTheExpectedKinds() {
		assertKinds(
				EntityLockingStrategies.standard(),
				LockMode.OPTIMISTIC,
				EntityLockingStrategyKind.STANDARD
		);
		assertKinds(
				EntityLockingStrategies.pessimisticSelect(),
				LockMode.PESSIMISTIC_READ,
				EntityLockingStrategyKind.SELECT
		);
		assertKinds(
				EntityLockingStrategies.pessimisticSelect(),
				LockMode.UPGRADE_SKIPLOCKED,
				EntityLockingStrategyKind.SELECT
		);
		assertKinds(
				EntityLockingStrategies.pessimisticUpdate(),
				LockMode.PESSIMISTIC_WRITE,
				EntityLockingStrategyKind.UPDATE
		);
		assertKinds(
				EntityLockingStrategies.pessimisticUpdate(),
				LockMode.UPGRADE_NOWAIT,
				EntityLockingStrategyKind.UPDATE
		);
	}

	@Test
	void requestExposesOnlyStableTargetFactsAndPropagatesScope() throws Exception {
		final EntityPersister persister = mock( EntityPersister.class );
		when( persister.getEntityName() ).thenReturn( "Book" );
		when( persister.isVersioned() ).thenReturn( true );
		final EntityLockingStrategyRequestImpl request = new EntityLockingStrategyRequestImpl(
				persister,
				LockMode.PESSIMISTIC_WRITE,
				PessimisticLockScope.EXTENDED
		);

		assertThat( request.target().entityName() ).isEqualTo( "Book" );
		assertThat( request.target().versioned() ).isTrue();
		assertThat( request.target() ).isNotInstanceOf( EntityPersister.class );
		assertThat( request.lockScope() ).isEqualTo( PessimisticLockScope.EXTENDED );

		final LockingStrategy strategy = request.createStrategy( EntityLockingStrategyKind.SQL_AST );
		assertThat( strategy ).isInstanceOf( SqlAstBasedLockingStrategy.class );
		final Field scope = SqlAstBasedLockingStrategy.class.getDeclaredField( "lockScope" );
		scope.setAccessible( true );
		assertThat( scope.get( strategy ) ).isEqualTo( PessimisticLockScope.EXTENDED );
	}

	@Test
	void requestRejectsInvalidKindsAndTargets() {
		final EntityPersister unversioned = mock( EntityPersister.class );
		when( unversioned.getEntityName() ).thenReturn( "LogEntry" );
		final EntityLockingStrategyRequestImpl readRequest = new EntityLockingStrategyRequestImpl(
				unversioned,
				LockMode.READ,
				PessimisticLockScope.NORMAL
		);

		assertThatIllegalArgumentException()
				.isThrownBy( () -> readRequest.createStrategy( EntityLockingStrategyKind.UPDATE ) )
				.withMessageContaining( "LogEntry" )
				.withMessageContaining( "no version" );
		assertThat( org.assertj.core.api.Assertions.catchThrowable( () -> readRequest.createStrategy( null ) ) )
				.isInstanceOf( NullPointerException.class );

		final EntityLockingStrategyRequestImpl noneRequest = new EntityLockingStrategyRequestImpl(
				unversioned,
				LockMode.NONE,
				PessimisticLockScope.NORMAL
		);
		assertThatIllegalArgumentException()
				.isThrownBy( () -> noneRequest.createStrategy( EntityLockingStrategyKind.STANDARD ) )
				.withMessageContaining( "NONE" );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> readRequest.createStrategy( EntityLockingStrategyKind.SQL_AST ) )
				.withMessageContaining( "READ" )
				.withMessageContaining( "SQL_AST" );
		assertThatNullPointerException()
				.isThrownBy( () -> EntityLockingStrategies.standard().createStrategy( null ) )
				.withMessageContaining( "request" );
		assertThatNullPointerException()
				.isThrownBy( () -> new EntityLockingStrategyRequestImpl(
						null,
						LockMode.READ,
						PessimisticLockScope.NORMAL
				) )
				.withMessageContaining( "persister" );
		assertThatNullPointerException()
				.isThrownBy( () -> new EntityLockingStrategyRequestImpl(
						unversioned,
						null,
						PessimisticLockScope.NORMAL
				) )
				.withMessageContaining( "mode" );
		assertThatNullPointerException()
				.isThrownBy( () -> new EntityLockingStrategyRequestImpl(
						unversioned,
						LockMode.READ,
						null
				) )
				.withMessageContaining( "scope" );
	}

	@Test
	void createsEverySupportedStockKindAndMode(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( VersionedEntity.class );

		assertStrategy( persister, LockMode.READ, EntityLockingStrategyKind.STANDARD, SelectLockingStrategy.class );
		assertStrategy( persister, LockMode.OPTIMISTIC, EntityLockingStrategyKind.STANDARD, OptimisticLockingStrategy.class );
		assertStrategy(
				persister,
				LockMode.OPTIMISTIC_FORCE_INCREMENT,
				EntityLockingStrategyKind.STANDARD,
				OptimisticForceIncrementLockingStrategy.class
		);
		assertStrategy(
				persister,
				LockMode.PESSIMISTIC_FORCE_INCREMENT,
				EntityLockingStrategyKind.STANDARD,
				PessimisticForceIncrementLockingStrategy.class
		);

		for ( LockMode mode : List.of(
				LockMode.PESSIMISTIC_READ,
				LockMode.PESSIMISTIC_WRITE,
				LockMode.UPGRADE_NOWAIT,
				LockMode.UPGRADE_SKIPLOCKED
		) ) {
			assertStrategy( persister, mode, EntityLockingStrategyKind.STANDARD, SqlAstBasedLockingStrategy.class );
			assertStrategy( persister, mode, EntityLockingStrategyKind.SQL_AST, SqlAstBasedLockingStrategy.class );
		}

		assertStrategy( persister, LockMode.READ, EntityLockingStrategyKind.SELECT, SelectLockingStrategy.class );
		assertStrategy(
				persister,
				LockMode.PESSIMISTIC_READ,
				EntityLockingStrategyKind.SELECT,
				PessimisticReadSelectLockingStrategy.class
		);
		for ( LockMode mode : List.of(
				LockMode.PESSIMISTIC_WRITE,
				LockMode.UPGRADE_NOWAIT,
				LockMode.UPGRADE_SKIPLOCKED
		) ) {
			assertStrategy( persister, mode, EntityLockingStrategyKind.SELECT, PessimisticWriteSelectLockingStrategy.class );
		}

		assertStrategy(
				persister,
				LockMode.PESSIMISTIC_READ,
				EntityLockingStrategyKind.UPDATE,
				PessimisticReadUpdateLockingStrategy.class
		);
		for ( LockMode mode : List.of(
				LockMode.PESSIMISTIC_WRITE,
				LockMode.UPGRADE_NOWAIT,
				LockMode.UPGRADE_SKIPLOCKED
		) ) {
			assertStrategy( persister, mode, EntityLockingStrategyKind.UPDATE, PessimisticWriteUpdateLockingStrategy.class );
		}

		assertThatIllegalArgumentException()
				.isThrownBy( () -> request( persister, LockMode.OPTIMISTIC )
						.createStrategy( EntityLockingStrategyKind.SELECT ) )
				.withMessageContaining( "OPTIMISTIC" )
				.withMessageContaining( "SELECT" );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> request( persister, LockMode.PESSIMISTIC_FORCE_INCREMENT )
						.createStrategy( EntityLockingStrategyKind.UPDATE ) )
				.withMessageContaining( "PESSIMISTIC_FORCE_INCREMENT" )
				.withMessageContaining( "UPDATE" );
	}

	@Test
	void requestImplementationIsImmutable() {
		assertThat( Modifier.isFinal( EntityLockingStrategyRequestImpl.class.getModifiers() ) ).isTrue();
		assertThat( EntityLockingStrategyRequestImpl.class.getDeclaredFields() )
				.filteredOn( field -> !Modifier.isStatic( field.getModifiers() ) )
				.allMatch( field -> Modifier.isFinal( field.getModifiers() ) );
	}

	@Test
	void persisterCachesNormalScopeAndRejectsNullFactoryResults(SessionFactoryScope scope) {
		final EntityPersister first = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( VersionedEntity.class );
		final EntityPersister second = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( UnversionedEntity.class );

		FACTORY_INVOCATIONS.set( 0 );
		suppliedFactory = RECORDING_FACTORY;
		final LockingStrategy normal = locker( first, LockMode.PESSIMISTIC_READ, PessimisticLockScope.NORMAL );
		assertThat( locker( first, LockMode.PESSIMISTIC_READ, PessimisticLockScope.NORMAL ) ).isSameAs( normal );
		final LockingStrategy firstExtended = locker(
				first,
				LockMode.PESSIMISTIC_READ,
				PessimisticLockScope.EXTENDED
		);
		final LockingStrategy secondExtended = locker(
				first,
				LockMode.PESSIMISTIC_READ,
				PessimisticLockScope.EXTENDED
		);
		assertThat( secondExtended ).isNotSameAs( firstExtended );
		assertThat( locker( second, LockMode.PESSIMISTIC_READ, PessimisticLockScope.NORMAL ) ).isNotSameAs( normal );
		assertThat( FACTORY_INVOCATIONS ).hasValue( 4 );

		try {
			suppliedFactory = null;
			assertThatNullPointerException()
					.isThrownBy( () -> locker( first, LockMode.OPTIMISTIC, PessimisticLockScope.NORMAL ) )
					.withMessageContaining( "factory" );
			suppliedFactory = request -> null;
			assertThatNullPointerException()
					.isThrownBy( () -> locker( first, LockMode.OPTIMISTIC_FORCE_INCREMENT, PessimisticLockScope.NORMAL ) )
					.withMessageContaining( "returned null" );
		}
		finally {
			suppliedFactory = RECORDING_FACTORY;
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void extendedAcquisitionUsesCustomFactoryAndEffectiveMode(boolean modeUpgrade, SessionFactoryScope scope) {
		final var persister = spy( scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( VersionedEntity.class ) );
		final var entry = mock( EntityEntry.class );
		final var session = mock( SharedSessionContractImplementor.class );
		final var entity = new VersionedEntity();
		when( entry.getLockMode() ).thenReturn( modeUpgrade ? LockMode.READ : LockMode.PESSIMISTIC_FORCE_INCREMENT );
		when( entry.getPersister() ).thenReturn( persister );
		when( entry.getStatus() ).thenReturn( Status.MANAGED );
		when( entry.getId() ).thenReturn( 1L );
		when( entry.getVersion() ).thenReturn( 1L );
		when( entry.isExistsInDatabase() ).thenReturn( true );
		when( session.getEventMonitor() ).thenReturn( mock( EventMonitor.class ) );
		final List<LockMode> requestedModes = new ArrayList<>();
		final List<Timeout> timeouts = new ArrayList<>();
		doAnswer( invocation -> {
			assertThat( requestedModes ).containsExactly( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE );
			assertThat( timeouts ).hasSize( 2 );
			return 2L;
		} ).when( persister ).forceVersionIncrement( 1L, 1L, session );
		try {
			suppliedFactory = request -> {
				requestedModes.add( request.lockMode() );
				assertThat( request.lockScope() ).isEqualTo( PessimisticLockScope.EXTENDED );
				assertThat( request.target().entityName() ).isEqualTo( persister.getEntityName() );
				return (id, version, object, timeout, source) -> {
					assertThat( id ).isEqualTo( 1L );
					assertThat( version ).isEqualTo( 1L );
					assertThat( object ).isSameAs( entity );
					assertThat( source ).isSameAs( session );
					timeouts.add( timeout );
				};
			};
			for ( LockMode requested : List.of( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_FORCE_INCREMENT ) ) {
				final var options = new LockOptions( requested ).setLockScope( PessimisticLockScope.EXTENDED )
						.setTimeout( Timeout.milliseconds( 750 ) );
				LoaderHelper.upgradeLock( entity, entry, options, session );
				assertThat( options.getLockMode() ).isEqualTo( requested );
			}
			assertThat( requestedModes ).containsExactly( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE );
			assertThat( timeouts ).containsExactly( Timeout.milliseconds( 750 ), Timeout.milliseconds( 750 ) );
			verify( persister, times( modeUpgrade ? 1 : 0 ) ).forceVersionIncrement( 1L, 1L, session );
		}
		finally {
			suppliedFactory = RECORDING_FACTORY;
		}
	}

	private static void assertStrategy(
			EntityPersister persister,
			LockMode lockMode,
			EntityLockingStrategyKind kind,
			Class<? extends LockingStrategy> expectedType) {
		assertThat( request( persister, lockMode ).createStrategy( kind ) ).isExactlyInstanceOf( expectedType );
	}

	private static EntityLockingStrategyRequest request(EntityPersister persister, LockMode lockMode) {
		return new EntityLockingStrategyRequestImpl( persister, lockMode, PessimisticLockScope.NORMAL );
	}

	private static LockingStrategy locker(
			EntityPersister persister,
			LockMode lockMode,
			PessimisticLockScope scope) {
		try {
			final Method method = AbstractEntityPersister.class.getDeclaredMethod(
					"getLocker",
					LockMode.class,
					PessimisticLockScope.class
			);
			method.setAccessible( true );
			return (LockingStrategy) method.invoke( persister, lockMode, scope );
		}
		catch (InvocationTargetException exception) {
			if ( exception.getCause() instanceof RuntimeException runtimeException ) {
				throw runtimeException;
			}
			throw new AssertionError( exception.getCause() );
		}
		catch (ReflectiveOperationException exception) {
			throw new AssertionError( exception );
		}
	}

	private static void assertKinds(
			EntityLockingStrategyFactory factory,
			LockMode lockMode,
			EntityLockingStrategyKind expectedKind) {
		final List<EntityLockingStrategyKind> kinds = new ArrayList<>();

		factory.createStrategy( request( lockMode, kinds ) );

		assertThat( kinds ).containsExactly( expectedKind );
	}

	private static EntityLockingStrategyRequest request(
			LockMode lockMode,
			List<EntityLockingStrategyKind> kinds) {
		final EntityLockTarget target = proxy(
				EntityLockTarget.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "entityName" -> "Book";
					case "versioned" -> true;
					default -> defaultValue( method.getReturnType() );
				}
		);
		return proxy(
				EntityLockingStrategyRequest.class,
				(method, arguments) -> switch ( method.getName() ) {
					case "target" -> target;
					case "lockMode" -> lockMode;
					case "lockScope" -> PessimisticLockScope.NORMAL;
					case "createStrategy" -> {
						kinds.add( (EntityLockingStrategyKind) arguments[0] );
						yield (LockingStrategy) (id, version, object, timeout, session) -> {
						};
					}
					default -> defaultValue( method.getReturnType() );
				}
		);
	}

	private static <T> T proxy(Class<T> contract, Invocation invocation) {
		return contract.cast( Proxy.newProxyInstance(
				EntityLockingStrategyFactoryTest.class.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> invocation.invoke( method, arguments )
		) );
	}

	private static Object defaultValue(Class<?> type) {
		return type == boolean.class ? false : null;
	}

	@FunctionalInterface
	private interface Invocation {
		Object invoke(java.lang.reflect.Method method, Object[] arguments);
	}

	public static class DialectSettingProvider implements SettingProvider.Provider<Dialect> {
		@Override
		public Dialect getSetting() {
			return new RecordingDialect();
		}
	}

	public static class RecordingDialect extends H2Dialect {
		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
			return suppliedFactory;
		}
	}

	private static final class RecordedStrategy implements LockingStrategy {
		private RecordedStrategy() {
			FACTORY_INVOCATIONS.incrementAndGet();
		}

		@Override
		public void lock(
				Object id,
				Object version,
				Object object,
				jakarta.persistence.Timeout timeout,
				org.hibernate.engine.spi.SharedSessionContractImplementor session) {
		}
	}

	@Entity(name = "VersionedLockTarget")
	public static class VersionedEntity {
		@Id
		private Long id;

		@Version
		private long version;
	}

	@Entity(name = "UnversionedLockTarget")
	public static class UnversionedEntity {
		@Id
		private Long id;
	}
}
