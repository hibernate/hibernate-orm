/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.internal.annotations.JandexAccess;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public abstract class BaseDelegatingBindingContext implements BindingContext {
	private final BindingContext parent;

	public BaseDelegatingBindingContext(BindingContext parent) {
		this.parent = parent;
	}

	protected BindingContext parent() {
		return parent;
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return parent.getBuildingOptions();
	}

	@Override
	public JandexAccess getJandexAccess() {
		return parent.getJandexAccess();
	}

	@Override
	public JavaTypeDescriptorRepository getJavaTypeDescriptorRepository() {
		return parent.getJavaTypeDescriptorRepository();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return parent.getClassLoaderAccess();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return parent.getMappingDefaults();
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return parent.getMetadataCollector();
	}

	@Override
	public MetaAttributeContext getGlobalMetaAttributeContext() {
		return parent.getGlobalMetaAttributeContext();
	}

	@Override
	public String qualifyClassName(String name) {
		return parent.qualifyClassName( name );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return parent.getServiceRegistry();
	}

	@Override
	public boolean quoteIdentifiersInContext() {
		return parent.quoteIdentifiersInContext();
	}

	@Override
	public BasicType buildBasicDomainType(JavaTypeDescriptor typeDescriptor) {
		return parent().buildBasicDomainType( typeDescriptor );
	}

	@Override
	public MappedSuperclass buildMappedSuperclassDomainType(JavaTypeDescriptor typeDescriptor) {
		return parent().buildMappedSuperclassDomainType( typeDescriptor );
	}

	@Override
	public MappedSuperclass buildMappedSuperclassDomainType(
			JavaTypeDescriptor typeDescriptor, Hierarchical superType) {
		return parent().buildMappedSuperclassDomainType( typeDescriptor, superType );
	}

	@Override
	public Aggregate buildComponentDomainType(JavaTypeDescriptor typeDescriptor) {
		return parent().buildComponentDomainType( typeDescriptor );
	}

	@Override
	public Aggregate buildComponentDomainType(
			JavaTypeDescriptor typeDescriptor, Hierarchical superType) {
		return parent().buildComponentDomainType( typeDescriptor, superType );
	}

	@Override
	public Entity buildEntityDomainType(JavaTypeDescriptor typeDescriptor) {
		return parent().buildEntityDomainType( typeDescriptor );
	}

	@Override
	public Entity buildEntityDomainType(JavaTypeDescriptor typeDescriptor, Hierarchical superType) {
		return parent().buildEntityDomainType( typeDescriptor, superType );
	}

	@Override
	public Type locateDomainType(JavaTypeDescriptor typeDescriptor) {
		return parent().locateDomainType( typeDescriptor );
	}

	@Override
	public Type locateOrBuildDomainType(JavaTypeDescriptor typeDescriptor, boolean isAggregate) {
		return parent().locateOrBuildDomainType( typeDescriptor, isAggregate );
	}

	@Override
	public Type makeDomainType(String className) {
		return parent.makeDomainType( className );
	}

	@Override
	public JavaTypeDescriptor typeDescriptor(String name) {
		return parent.typeDescriptor( name );
	}

	@Override
	public Type makeDomainType(DotName typeName) {
		return parent.makeDomainType( typeName );
	}
}
