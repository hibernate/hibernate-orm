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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.spi.BaseDelegatingBindingContext;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

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
public class AnnotationBindingContextImpl
		extends BaseDelegatingBindingContext
		implements AnnotationBindingContext {

	private final TypeResolver classmateTypeResolver = new TypeResolver();
	private final MemberResolver classmateMemberResolver = new MemberResolver( classmateTypeResolver );

	private final IndexView index;
	private final ClassLoaderService classLoaderService;

	public AnnotationBindingContextImpl(BindingContext rootBindingContext, IndexView index) {
		super( rootBindingContext );
		this.classLoaderService = rootBindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
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
		return classmateMemberResolver;
	}

	@Override
	public TypeResolver getTypeResolver() {
		return classmateTypeResolver;
	}

	@Override
	public IdentifierGeneratorDefinition findIdGenerator(String name) {
		return getMetadataCollector().getIdGenerator( name );
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return classLoaderService.classForName( name );
	}
}
