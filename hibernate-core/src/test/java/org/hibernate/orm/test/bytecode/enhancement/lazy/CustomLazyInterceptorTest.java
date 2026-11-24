package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.type.CompositeType;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.persister.internal.PersisterClassResolverInitiator.IMPL_NAME;

/**
 * An integrator that extends {@link BytecodeEnhancementMetadataPojoImpl} to create its own interceptors
 * gets them for every instance of an enhanced entity, however Hibernate creates it.
 */
@DomainModel(annotatedClasses = CustomLazyInterceptorTest.Book.class)
@ServiceRegistry(settings = @Setting(name = IMPL_NAME,
		value = "org.hibernate.orm.test.bytecode.enhancement.lazy.CustomLazyInterceptorTest$Resolver"))
@SessionFactory
@BytecodeEnhanced
public class CustomLazyInterceptorTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Book( 1L, "Hibernate in Action", "A long text" ) ) );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = session.find( Book.class, 1L );
			assertThat( Hibernate.isPropertyInitialized( book, "text" ) ).isFalse();
			assertThat( interceptor( book ) ).isInstanceOf( CustomLazyAttributeLoadingInterceptor.class );
		} );
	}

	@Test
	public void testInstantiate(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( Book.class );
		final Object book = persister.getRepresentationStrategy().getInstantiator().instantiate();
		assertThat( interceptor( book ) ).isInstanceOf( CustomLazyAttributeLoadingInterceptor.class );
	}

	@Test
	public void testEnhancedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = session.getReference( Book.class, 1L );
			assertThat( Hibernate.isInitialized( book ) ).isFalse();
			assertThat( interceptor( book ) ).isInstanceOf( CustomEnhancementAsProxyLazinessInterceptor.class );
		} );
	}

	private static Object interceptor(Object entity) {
		return asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		Long id;

		String title;

		@Basic(fetch = FetchType.LAZY)
		String text;

		Book() {
		}

		Book(Long id, String title, String text) {
			this.id = id;
			this.title = title;
			this.text = text;
		}
	}

	public static class Resolver extends StandardPersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass model) {
			return CustomPersister.class;
		}
	}

	public static class CustomPersister extends SingleTableEntityPersister {
		public CustomPersister(
				PersistentClass persistentClass,
				EntityDataAccess cacheAccessStrategy,
				NaturalIdDataAccess naturalIdRegionAccessStrategy,
				RuntimeModelCreationContext creationContext) {
			super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
		}

		@Override
		protected BytecodeEnhancementMetadata getBytecodeEnhancementMetadataPojo(
				PersistentClass persistentClass,
				RuntimeModelCreationContext creationContext,
				Set<String> idAttributeNames,
				CompositeType nonAggregatedCidMapper,
				boolean collectionsInDefaultFetchGroupEnabled) {
			return new CustomBytecodeEnhancementMetadata(
					persistentClass.getEntityName(),
					persistentClass.getMappedClass(),
					idAttributeNames,
					nonAggregatedCidMapper,
					LazyAttributesMetadata.from(
							persistentClass,
							true,
							collectionsInDefaultFetchGroupEnabled,
							creationContext.getMetadata()
					)
			);
		}
	}

	public static class CustomBytecodeEnhancementMetadata extends BytecodeEnhancementMetadataPojoImpl {
		CustomBytecodeEnhancementMetadata(
				String entityName,
				Class<?> entityClass,
				Set<String> identifierAttributeNames,
				CompositeType nonAggregatedCidMapper,
				LazyAttributesMetadata lazyAttributesMetadata) {
			super( entityName, entityClass, identifierAttributeNames, nonAggregatedCidMapper, true, lazyAttributesMetadata );
		}

		@Override
		public LazyAttributeLoadingInterceptor createLazyAttributeLoadingInterceptor(
				Object identifier,
				SharedSessionContractImplementor session) {
			return new CustomLazyAttributeLoadingInterceptor( getLazyAttributeLoadingInterceptorState(), identifier, session );
		}

		@Override
		protected EnhancementAsProxyLazinessInterceptor createEnhancementAsProxyLazinessInterceptor(
				EntityKey entityKey,
				SharedSessionContractImplementor session) {
			return new CustomEnhancementAsProxyLazinessInterceptor(
					getEnhancementAsProxyLazinessInterceptorMetastate( session ),
					entityKey,
					session
			);
		}
	}

	public static class CustomLazyAttributeLoadingInterceptor extends LazyAttributeLoadingInterceptor {
		CustomLazyAttributeLoadingInterceptor(
				EntityRelatedState entityMeta,
				Object identifier,
				SharedSessionContractImplementor session) {
			super( entityMeta, identifier, session );
		}
	}

	public static class CustomEnhancementAsProxyLazinessInterceptor extends EnhancementAsProxyLazinessInterceptor {
		CustomEnhancementAsProxyLazinessInterceptor(
				EntityRelatedState meta,
				EntityKey entityKey,
				SharedSessionContractImplementor session) {
			super( meta, entityKey, session );
		}
	}
}
