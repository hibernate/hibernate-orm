package org.hibernate.orm.test.metamodel.representation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A resolver supplied through {@link MappingSettings#MANAGED_TYPE_REPRESENTATION_RESOLVER} is used
 * for every managed type, and can delegate to the standard resolver.
 */
@JiraKey("HHH-20967")
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.MANAGED_TYPE_REPRESENTATION_RESOLVER,
				value = "org.hibernate.orm.test.metamodel.representation.CustomRepresentationResolverTests$RecordingResolver")
})
@DomainModel(xmlMappings = "org/hibernate/orm/test/bootstrap/binding/hbm/simple/dynamic/SimpleDynamicEntity.xml")
@SessionFactory
public class CustomRepresentationResolverTests {

	@Test
	public void customResolverIsConsulted(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();
		assertThat( RecordingResolver.ENTITIES, hasItem( "SimpleDynamicEntity" ) );
	}

	@Test
	public void delegatedTypesBehaveAsStandard(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			final List<Map> rows = session.createQuery( "from SimpleDynamicEntity", Map.class ).list();
			assertThat( rows.isEmpty(), is( true ) );
		} );
	}

	public static class RecordingResolver implements ManagedTypeRepresentationResolver {
		static final List<String> ENTITIES = new CopyOnWriteArrayList<>();

		@Override
		public EntityRepresentationStrategy resolveStrategy(
				PersistentClass bootDescriptor,
				EntityPersister runtimeDescriptor,
				RuntimeModelCreationContext creationContext) {
			ENTITIES.add( bootDescriptor.getEntityName() );
			return ManagedTypeRepresentationResolverStandard.INSTANCE
					.resolveStrategy( bootDescriptor, runtimeDescriptor, creationContext );
		}

		@Override
		public EmbeddableRepresentationStrategy resolveStrategy(
				Component bootDescriptor,
				Supplier<EmbeddableMappingType> runtimeDescriptorAccess,
				RuntimeModelCreationContext creationContext) {
			return ManagedTypeRepresentationResolverStandard.INSTANCE
					.resolveStrategy( bootDescriptor, runtimeDescriptorAccess, creationContext );
		}
	}
}
