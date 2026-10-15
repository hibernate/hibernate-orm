package org.hibernate.orm.test.cdi.guide;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Executes the managed-bean configurations included in the User Guide.
///
/// @author Steve Ebersole
@BaseUnitTest
class ManagedBeansGuideTests {

	@Test
	@CdiContainer(beanClasses = { AmountConverter.class, ConversionService.class })
	void converterInjectionAndDestruction(CdiContainerScope cdi) {
		var configuration = cdiConfiguration( cdi.getBeanManager() );
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		configuration.property( "hibernate.cdi.extensions", false );
		var monitor = cdi.getContainer().select( ConversionService.class ).get();
		monitor.writes.set( 0 );
		monitor.reads.set( 0 );
		monitor.initializations.set( 0 );
		monitor.destructions.set( 0 );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new Stock( 1L, 23 ) ) );
			factory.inTransaction( session -> assertThat( session.find( Stock.class, 1L ).amount ).isEqualTo( 23 ) );
			assertThat( monitor.writes ).hasPositiveValue();
			assertThat( monitor.reads ).hasPositiveValue();
			assertThat( monitor.initializations ).hasPositiveValue();
			assertThat( monitor.destructions ).hasValue( 0 );
		}
		assertThat( monitor.destructions.get() ).isEqualTo( monitor.initializations.get() );
		assertThat( cdi.getContainer().isRunning() ).isTrue();
	}

	// tag::beans-cdi-configuration[]
	static HibernatePersistenceConfiguration cdiConfiguration(BeanManager beanManager) {
		return new HibernatePersistenceConfiguration( "managed-converter" )
				.managedClasses( Stock.class )
				.property( "jakarta.persistence.bean.manager", beanManager );
	}
	// end::beans-cdi-configuration[]

	@ParameterizedTest
	@ValueSource(strings = { "instance", "class", "name" })
	void explicitContainerRegistration(String form) {
		var supplied = new GuideBeanContainer();
		Object setting = switch ( form ) {
			case "instance" -> supplied;
			case "class" -> GuideBeanContainer.class;
			default -> GuideBeanContainer.class.getName();
		};
		final GuideBeanContainer selected;
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.resource.beans.container", setting ).build() ) {
			var registry = services.requireService( ManagedBeanRegistry.class );
			selected = (GuideBeanContainer) registry.getBeanContainer();
			if ( form.equals( "instance" ) ) {
				assertThat( selected ).isSameAs( supplied );
			}
			assertThat( registry.getBean( PlainBean.class ).getBeanInstance() ).isInstanceOf( PlainBean.class );
			assertThat( selected.stopped ).isFalse();
		}
		assertThat( selected.stopped ).isTrue();
	}

	@Test
	void applicationManagedContainerConfiguration() {
		var container = new GuideBeanContainer();
		var configuration = customConfiguration( container );
		configuration.properties( ServiceRegistryUtil.createBaseSettingsWithExport() );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new PlainEntity() ) );
		}
		assertThat( container.stopped ).isTrue();
	}

	// tag::beans-custom-configuration[]
	static HibernatePersistenceConfiguration customConfiguration(BeanContainer beanContainer) {
		return new HibernatePersistenceConfiguration( "custom-bean-container" )
				.managedClasses( PlainEntity.class )
				.property( "hibernate.resource.beans.container", beanContainer );
	}
	// end::beans-custom-configuration[]

	@Test
	void serviceDiscoveryAndExplicitOverride(@TempDir Path directory) throws Exception {
		var descriptor = directory.resolve( "META-INF/services/" + BeanContainer.class.getName() );
		Files.createDirectories( descriptor.getParent() );
		try ( var resource = getClass().getResourceAsStream( "/beans-guide/bean-container-service.txt" ) ) {
			Files.copy( resource, descriptor );
		}
		try ( var loader = new URLClassLoader( new java.net.URL[] { directory.toUri().toURL() }, getClass().getClassLoader() ) ) {
			try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
					var services = new StandardServiceRegistryBuilder( bootstrap ).build() ) {
				assertThat( services.requireService( ManagedBeanRegistry.class ).getBeanContainer() )
						.isInstanceOf( GuideBeanContainer.class );
			}
			Files.writeString( descriptor, GuideBeanContainer.class.getName() + "\n" + OtherContainer.class.getName() + "\n" );
			try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
					var services = new StandardServiceRegistryBuilder( bootstrap ).build() ) {
				assertThatThrownBy( () -> services.requireService( ManagedBeanRegistry.class ) )
						.isInstanceOf( ServiceException.class ).hasStackTraceContaining( "Multiple BeanContainer" );
			}
			var explicit = new GuideBeanContainer();
			try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
					var services = new StandardServiceRegistryBuilder( bootstrap )
						.applySetting( "hibernate.resource.beans.container", explicit )
						.applySetting( "jakarta.persistence.bean.manager", new Object() ).build() ) {
				assertThat( services.requireService( ManagedBeanRegistry.class ).getBeanContainer() ).isSameAs( explicit );
			}
		}
	}

	public static class OtherContainer extends GuideBeanContainer {
	}

	public static class PlainBean {
	}

	@Entity(name = "BeansGuidePlain")
	public static class PlainEntity {
		@Id
		Long id = 1L;
	}

	// tag::beans-cdi-model[]
	@Entity(name = "BeansGuideStock")
	public static class Stock {
		@Id
		Long id;

		@Convert(converter = AmountConverter.class)
		Integer amount;

		public Stock() {
		}

		Stock(Long id, Integer amount) {
			this.id = id;
			this.amount = amount;
		}
	}
	// end::beans-cdi-model[]

	// tag::beans-cdi-converter[]
	@Converter
	public static class AmountConverter implements AttributeConverter<Integer, String> {
		@Inject
		ConversionService service;

		@PostConstruct
		void initialized() {
			service.initialized();
		}

		@PreDestroy
		void destroyed() {
			service.destroyed();
		}

		@Override
		public String convertToDatabaseColumn(Integer value) {
			return service.encode( value );
		}

		@Override
		public Integer convertToEntityAttribute(String value) {
			return service.decode( value );
		}
	}
	// end::beans-cdi-converter[]

	// tag::beans-cdi-service[]
	@jakarta.inject.Singleton
	public static class ConversionService {
		final AtomicInteger writes = new AtomicInteger();
		final AtomicInteger reads = new AtomicInteger();
		final AtomicInteger initializations = new AtomicInteger();
		final AtomicInteger destructions = new AtomicInteger();

		public void initialized() {
			initializations.incrementAndGet();
		}

		public void destroyed() {
			destructions.incrementAndGet();
		}

		public String encode(Integer value) {
			writes.incrementAndGet();
			return value == null ? null : value.toString();
		}

		public Integer decode(String value) {
			reads.incrementAndGet();
			return value == null ? null : Integer.valueOf( value );
		}
	}
	// end::beans-cdi-service[]
}
