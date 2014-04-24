/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.internal.annotations.JandexAccess;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.JavaClassReference;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.DotName;

/**
 * Describes the context in which binding (the process of build Metadata out of
 * MetadataSources) occurs.
 * <p/>
 * BindingContext are generally hierarchical getting more specific as we "go
 * down".  E.g.  global -> PU -> document -> mapping
 *
 * @author Steve Ebersole
 */
public interface BindingContext {
	/**
	 * Access to the options specified by the {@link org.hibernate.metamodel.MetadataBuilder}
	 *
	 * @return The options
	 */
	public MetadataBuildingOptions getBuildingOptions();

	public JandexAccess getJandexAccess();

	/**
	 * Access to the "reflite" type repository
	 *
	 * @return The reflite type repo
	 */
	public JavaTypeDescriptorRepository getJavaTypeDescriptorRepository();

	/**
	 * Access to mapping defaults in effect for this context
	 *
	 * @return The mapping defaults.
	 */
	public MappingDefaults getMappingDefaults();

	/**
	 * Access to the collector of metadata as we build it.
	 *
	 * @return The metadata collector.
	 */
	public InFlightMetadataCollector getMetadataCollector();

	/**
	 * Shortcut for {@link #getBuildingOptions()} -> {@link org.hibernate.metamodel.spi.MetadataBuildingOptions#getServiceRegistry()}
	 *
	 * @return The ServiceRegistry
	 */
	public ServiceRegistry getServiceRegistry();

	/**
	 * Provides access to ClassLoader services when needed during binding
	 *
	 * @return The ClassLoaderAccess
	 */
	public ClassLoaderAccess getClassLoaderAccess();

	public MetaAttributeContext getGlobalMetaAttributeContext();

	/**
	 * Qualify a class name per the rules for this context
	 *
	 * @param name The class name
	 *
	 * @return The qualified name
	 */
	public String qualifyClassName(String name);

	public boolean quoteIdentifiersInContext();

	public JavaTypeDescriptor typeDescriptor(String name);

	public BasicType buildBasicDomainType(JavaTypeDescriptor typeDescriptor);
	public MappedSuperclass buildMappedSuperclassDomainType(JavaTypeDescriptor typeDescriptor);
	public MappedSuperclass buildMappedSuperclassDomainType(JavaTypeDescriptor typeDescriptor, Hierarchical superType);
	public Aggregate buildComponentDomainType(JavaTypeDescriptor typeDescriptor);
	public Aggregate buildComponentDomainType(JavaTypeDescriptor typeDescriptor, Hierarchical superType);
	public Entity buildEntityDomainType(JavaTypeDescriptor typeDescriptor);
	public Entity buildEntityDomainType(JavaTypeDescriptor typeDescriptor, Hierarchical superType);

	public Type locateDomainType(JavaTypeDescriptor typeDescriptor);

	/**
	 * It is expected that Entity types already exist and are known.  Therefore,
	 * in the "build" case, the expectation is that the domain type is either
	 * a basic type or a component.
	 *
	 * @param typeDescriptor The Java type descriptor
	 *
	 * @return The domain type.
	 */
	public Type locateOrBuildDomainType(JavaTypeDescriptor typeDescriptor, boolean isAggregate);

	// todo : go away

	/**
	 * todo : maybe a `Type makeDomainType(JavaTypeDescriptor)` method instead?
	 *
	 * @deprecated use the JavaTypeDescriptorRepository instead, available from {@link #getJavaTypeDescriptorRepository}
	 */
	@Deprecated
	public Type makeDomainType(String className);

	public Type makeDomainType(DotName typeName);

}
