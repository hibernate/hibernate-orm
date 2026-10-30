package org.hibernate.orm.test.bootstrap;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that state only needed for binding the boot model is not kept
 * reachable from the {@link SessionFactory} once it has been created.
 */
@BaseUnitTest
@JiraKey("HHH-20837")
public class BootBindingStateReleaseTest {

	private static final Class<?>[] ENTITIES = { Book.class, Publisher.class };

	@Test
	public void testNativeBootstrap() throws Exception {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistry();
		try {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClasses( ENTITIES )
					.buildMetadata();
			final WeakReference<ModelsContext> modelsContext = modelsContext( metadata );
			try (SessionFactory sessionFactory = metadata.buildSessionFactory()) {
				assertReleased( sessionFactory, modelsContext );
				assertSchemaManagementWorks( sessionFactory );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	public void testReleaseDisabled() throws Exception {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( PersistenceSettings.RELEASE_BOOT_BINDING_STATE, false )
				.build();
		try {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClasses( ENTITIES )
					.buildMetadata();
			try (SessionFactory sessionFactory = metadata.buildSessionFactory()) {
				assertThat( ( (MetadataImpl) metadata ).getBootstrapContext().getModelsContext() ).isNotNull();
				assertSchemaManagementWorks( sessionFactory );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	public void testJpaBootstrap() throws Exception {
		final var configuration = new HibernatePersistenceConfiguration( "release-binding-state" )
				.managedClasses( ENTITIES )
				.properties( ServiceRegistryUtil.createBaseSettings() );
		final WeakReference<ModelsContext> modelsContext;
		final EntityManagerFactory entityManagerFactory;
		{
			final var builder = new EntityManagerFactoryBuilderImpl( configuration );
			modelsContext = modelsContext( builder.metadata() );
			entityManagerFactory = builder.build();
		}
		try (var sessionFactory = entityManagerFactory.unwrap( SessionFactory.class )) {
			assertReleased( sessionFactory, modelsContext );
			assertSchemaManagementWorks( sessionFactory );
		}
	}

	private static WeakReference<ModelsContext> modelsContext(MetadataImplementor metadata) {
		return new WeakReference<>( ( (MetadataImpl) metadata ).getBootstrapContext().getModelsContext() );
	}

	private static void assertReleased(SessionFactory sessionFactory, WeakReference<ModelsContext> modelsContext) {
		for ( int i = 0; i < 10 && modelsContext.get() != null; i++ ) {
			System.gc();
			try {
				Thread.sleep( 50 );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		assertThat( modelsContext.get() ).isNull();
	}

	private static void assertSchemaManagementWorks(SessionFactory sessionFactory) throws Exception {
		final var schemaManager = sessionFactory.getSchemaManager();
		schemaManager.create( true );
		schemaManager.validate();
		schemaManager.truncate();
		schemaManager.drop( true );
	}

	public enum Genre { FICTION, NON_FICTION }

	@Embeddable
	public static class Price {
		long amount;
		String currency;
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		Long id;
		@NaturalId
		String isbn;
		@NaturalId
		String edition;
		String title;
		@Enumerated(EnumType.STRING)
		Genre genre;
		@Embedded
		Price price;
		@ManyToOne
		Publisher publisher;
		@ElementCollection
		@OrderColumn
		List<String> tags;
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		@GeneratedValue
		Long id;
		String name;
		@CreationTimestamp
		Instant created;
		@OneToMany(mappedBy = "publisher")
		Set<Book> books;
	}
}
