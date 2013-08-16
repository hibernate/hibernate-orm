/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.SourceType;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.service.ServiceRegistry;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;

/**
 * Annotation version of a local binding context.
 * 
 * @author Steve Ebersole
 */
public class EntityBindingContext implements LocalBindingContext, AnnotationBindingContext {
	private final AnnotationBindingContext contextDelegate;
	private final Origin origin;

	public EntityBindingContext(AnnotationBindingContext contextDelegate, ConfiguredClass source) {
		this.contextDelegate = contextDelegate;
		this.origin = new Origin( SourceType.ANNOTATION, source.getName() );
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public MappingException makeMappingException(String message) {
		return new MappingException( message, getOrigin() );
	}

	@Override
	public MappingException makeMappingException(String message, Exception cause) {
		return new MappingException( message, cause, getOrigin() );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return contextDelegate.getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return contextDelegate.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return contextDelegate.getMappingDefaults();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return contextDelegate.getMetadataImplementor();
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return contextDelegate.locateClassByName( name );
	}

	@Override
	public Type makeJavaType(String className) {
		return contextDelegate.makeJavaType( className );
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return contextDelegate.isGloballyQuotedIdentifiers();
	}

	@Override
	public ValueHolder<Class<?>> makeClassReference(String className) {
		return contextDelegate.makeClassReference( className );
	}

	@Override
	public String qualifyClassName(String name) {
		return contextDelegate.qualifyClassName( name );
	}

	@Override
	public IndexView getIndex() {
		return contextDelegate.getIndex();
	}

	@Override
	public ClassInfo getClassInfo(String name) {
		return contextDelegate.getClassInfo( name );
	}

	@Override
	public MemberResolver getMemberResolver() {
		return contextDelegate.getMemberResolver();
	}

	@Override
	public TypeResolver getTypeResolver() {
		return contextDelegate.getTypeResolver();
	}

	@Override
	public IdentifierGeneratorDefinition findIdGenerator(String name) {
		return contextDelegate.findIdGenerator( name );
	}
}
