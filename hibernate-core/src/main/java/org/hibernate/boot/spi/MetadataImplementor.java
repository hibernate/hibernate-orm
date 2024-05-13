/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * The SPI-level {@link Metadata} contract.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataImplementor extends Metadata {
	/**
	 * Access to the options used to build this {@code Metadata}
	 *
	 * @return The {@link MetadataBuildingOptions}
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();

	/**
	 * Access to the {@link TypeConfiguration} belonging to the {@link BootstrapContext}
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to the {@link SqmFunctionRegistry} belonging to the {@link BootstrapContext}
	 */
	SqmFunctionRegistry getFunctionRegistry();

	NamedObjectRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory);

	@Incubating
	void orderColumns(boolean forceOrdering);

	void validate() throws MappingException;

	Set<MappedSuperclass> getMappedSuperclassMappingsCopy();

	void initSessionFactory(SessionFactoryImplementor sessionFactoryImplementor);

	void visitRegisteredComponents(Consumer<Component> consumer);

	Component getGenericComponent(Class<?> componentClass);

	DiscriminatorType<?> resolveEmbeddableDiscriminatorType(Class<?> embeddableClass, Supplier<DiscriminatorType<?>> supplier);
}
