package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = ForceIncrementCustomPersisterTest.VersionedEntity.class)
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10"),
		services = @ServiceRegistry.Service(role = PersisterClassResolver.class, impl = ForceIncrementCustomPersisterTest.CustomPersisterResolver.class)
)
@SessionFactory
@JiraKey("HHH-20417")
public class ForceIncrementCustomPersisterTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		persister( scope ).increments = 0;
		scope.inTransaction( session -> session.persist( new VersionedEntity( 1L ) ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testOptimisticIncrementUsesCustomOverride(SessionFactoryScope scope) {
		final var persister = persister( scope );
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.OPTIMISTIC_FORCE_INCREMENT );
			session.flush();
			assertThat( persister.increments ).isZero();
			assertThat( entity.version ).isZero();
			assertThat( databaseVersion( session ) ).isZero();
		} );
		assertThat( persister.increments ).isEqualTo( 1 );
		scope.inTransaction( session -> {
			assertThat( session.find( VersionedEntity.class, 1L ).version ).isEqualTo( 1L );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	@Test
	void testPessimisticIncrementExecutesImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = session.find( VersionedEntity.class, 1L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( persister( scope ).increments ).isZero();
			assertThat( entity.version ).isEqualTo( 1L );
			assertThat( databaseVersion( session ) ).isEqualTo( 1L );
		} );
	}

	private static CustomPersister persister(SessionFactoryScope scope) {
		return (CustomPersister) scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( VersionedEntity.class );
	}

	private static long databaseVersion(Session session) {
		return session.doReturningWork( connection -> {
			try ( var statement = connection.prepareStatement(
					"select version from custom_force_increment where id = 1" );
					var resultSet = statement.executeQuery() ) {
				assertThat( resultSet.next() ).isTrue();
				return resultSet.getLong( 1 );
			}
		} );
	}

	public static class CustomPersisterResolver extends StandardPersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> singleTableEntityPersister() {
			return CustomPersister.class;
		}
	}

	public static class CustomPersister extends SingleTableEntityPersister {
		private int increments;

		public CustomPersister(
				PersistentClass persistentClass,
				EntityDataAccess cacheAccessStrategy,
				NaturalIdDataAccess naturalIdRegionAccessStrategy,
				RuntimeModelCreationContext creationContext) {
			super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
		}

		@Override
		public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
			increments++;
			return super.forceVersionIncrement( id, currentVersion, session );
		}
	}

	@Entity(name = "CustomVersionedEntity")
	@Table(name = "custom_force_increment")
	public static class VersionedEntity {
		@Id
		private Long id;
		@Version
		private long version;

		public VersionedEntity() {
		}

		VersionedEntity(Long id) {
			this.id = id;
		}
	}
}
