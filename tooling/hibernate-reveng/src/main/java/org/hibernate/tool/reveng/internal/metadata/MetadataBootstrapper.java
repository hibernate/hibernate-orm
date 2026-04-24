/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Bootstraps Hibernate ORM {@link Metadata} from a list of
 * {@link ClassDetails} entities. Registers the entities in the
 * bootstrap pipeline's {@link MutableClassDetailsRegistry} and
 * uses {@link MetadataBuildingProcess#complete} to produce
 * a fully resolved {@link Metadata} instance.
 * <p>
 * This is the bridge that allows hibernate-tools to work entirely
 * with {@code ClassDetails} (the hibernate-models API) while still
 * producing the {@code Metadata} required by ORM schema tools
 * ({@code SchemaCreatorImpl}, {@code SchemaDropperImpl},
 * {@code SchemaMigrator}) and {@code SessionFactory}.
 *
 * @author Koen Aers
 */
public class MetadataBootstrapper {

	/**
	 * Builds {@link Metadata} from the given entities and properties.
	 * The returned {@link MetadataContext} is {@link AutoCloseable}
	 * and will destroy the {@link StandardServiceRegistry} when closed.
	 */
	public static MetadataContext bootstrap(List<ClassDetails> entities,
											Properties properties) {
		StandardServiceRegistry serviceRegistry =
				new StandardServiceRegistryBuilder()
						.applySettings(properties)
						.build();
		MetadataBuildingOptionsImpl options =
				new MetadataBuildingOptionsImpl(serviceRegistry);
		BootstrapContextImpl bootstrapContext =
				new BootstrapContextImpl(serviceRegistry, options);
		options.setBootstrapContext(bootstrapContext);

		// Register ClassDetails in the bootstrap's ClassDetailsRegistry,
		// filtering out @Entity classes that have no identifier — these
		// cannot be processed by MetadataBuildingProcess and would throw
		// AnnotationException. @Embeddable classes are always included.
		MutableClassDetailsRegistry classDetailsRegistry =
				(MutableClassDetailsRegistry) bootstrapContext
						.getModelsContext().getClassDetailsRegistry();
		List<String> classNames = new ArrayList<>();
		for (ClassDetails entity : entities) {
			if (isEntity(entity, bootstrapContext) && !hasIdentifier(entity)) {
				continue;
			}
			classDetailsRegistry.addClassDetails(
					entity.getClassName(), entity);
			classNames.add(entity.getClassName());
		}

		// Build ManagedResources with the entity class names
		ManagedResourcesImpl managedResources = new ManagedResourcesImpl();
		for (String className : classNames) {
			managedResources.addAnnotatedClassName(className);
		}

		Metadata metadata = MetadataBuildingProcess.complete(
				managedResources, bootstrapContext, options);
		return new MetadataContext(metadata, serviceRegistry);
	}

	private static boolean isEntity(ClassDetails cd,
								BootstrapContextImpl bootstrapContext) {
		return cd.hasAnnotationUsage(Entity.class,
				bootstrapContext.getModelsContext());
	}

	private static boolean hasIdentifier(ClassDetails cd) {
		for (FieldDetails field : cd.getFields()) {
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Holds the bootstrapped {@link Metadata} and the associated
	 * {@link StandardServiceRegistry}. Implements {@link AutoCloseable}
	 * to ensure the service registry is properly destroyed.
	 */
	public record MetadataContext(Metadata metadata,
								StandardServiceRegistry serviceRegistry)
			implements AutoCloseable {

		@Override
		public void close() {
			StandardServiceRegistryBuilder.destroy(serviceRegistry);
		}
	}
}
