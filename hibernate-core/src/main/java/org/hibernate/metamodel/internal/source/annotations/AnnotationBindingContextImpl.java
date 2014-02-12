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
package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.domain.JavaClassReference;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.service.ServiceRegistry;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;

/**
 * Default implementation of  {@code AnnotationBindingContext}
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class AnnotationBindingContextImpl implements AnnotationBindingContext {
	private static final TypeResolver TYPE_RESOLVER = new TypeResolver();
	private static final MemberResolver MEMBER_RESOLVER = new MemberResolver( TYPE_RESOLVER );
	private final MetadataImplementor metadata;
	private final ClassLoaderService classLoaderService;
	private final IndexView index;

	/**
	 * Constructor
	 *
	 * @param metadata {@code Metadata} instance
	 * @param index the Jandex index view
	 */
	public AnnotationBindingContextImpl(MetadataImplementor metadata, IndexView index) {
		this.metadata = metadata;
		this.classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		this.index = index;
	}

	@Override
	public IndexView getIndex() {
		return index;
	}

	@Override
	public ClassInfo getClassInfo(String name) {
		DotName dotName = DotName.createSimple( name );
		return index.getClassByName( dotName );
	}

	@Override
	public MemberResolver getMemberResolver() {
		return MEMBER_RESOLVER;
	}

	@Override
	public TypeResolver getTypeResolver() {
		return TYPE_RESOLVER;
	}

	@Override
	public IdentifierGeneratorDefinition findIdGenerator(String name) {
		return getMetadataImplementor().getIdGenerator( name );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadataImplementor().getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return metadata.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return metadata.getMappingDefaults();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return metadata;
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return classLoaderService.classForName( name );
	}

	@Override
	public Type makeDomainType(String className) {
		return  metadata.makeDomainType( className );
	}

	@Override
	public JavaClassReference makeJavaClassReference(String className) {
		return metadata.makeJavaClassReference( className );
	}

	@Override
	public JavaClassReference makeJavaClassReference(Class<?> clazz) {
		return metadata.makeJavaClassReference( clazz );
	}

	@Override
	public JavaClassReference makeJavaPropertyClassReference(
			JavaClassReference propertyContainerClassReference,
			String propertyName) {
		return metadata.makeJavaPropertyClassReference( propertyContainerClassReference, propertyName );
	}

	@Override
	public String qualifyClassName(String name) {
		return name;
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return metadata.isGloballyQuotedIdentifiers();
	}

}
