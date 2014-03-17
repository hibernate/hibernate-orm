/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class IdentifierGeneratorSourceContainerImpl implements IdentifierGeneratorSourceContainer {
	
	private final ClassLoaderService classLoaderService;
	
	public IdentifierGeneratorSourceContainerImpl(AnnotationBindingContext bindingContext) {
		this.classLoaderService = bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class );
	}
	
	private Collection<AnnotationInstance> resolveOrEmpty(DotName name) {
		Collection<AnnotationInstance> generatorSources = getAnnotations( name );
		return generatorSources == null ? Collections.<AnnotationInstance>emptyList() : generatorSources;
	}

	private final ValueHolder<Collection<AnnotationInstance>> sequenceGeneratorSources = new ValueHolder<Collection<AnnotationInstance>>(
			new ValueHolder.DeferredInitializer<Collection<AnnotationInstance>>() {
				@Override
				public Collection<AnnotationInstance> initialize() {
					return resolveOrEmpty( JPADotNames.SEQUENCE_GENERATOR );
				}
			}
	);

	@Override
	public Collection<AnnotationInstance> getSequenceGeneratorSources() {
		return sequenceGeneratorSources.getValue();
	}

	private final ValueHolder<Collection<AnnotationInstance>> tableGeneratorSources = new ValueHolder<Collection<AnnotationInstance>>(
			new ValueHolder.DeferredInitializer<Collection<AnnotationInstance>>() {
				@Override
				public Collection<AnnotationInstance> initialize() {
					return resolveOrEmpty( JPADotNames.TABLE_GENERATOR );
				}
			}
	);

	@Override
	public Collection<AnnotationInstance> getTableGeneratorSources() {
		return tableGeneratorSources.getValue();
	}

	private final ValueHolder<Collection<AnnotationInstance>> genericGeneratorSources = new ValueHolder<Collection<AnnotationInstance>>(
			new ValueHolder.DeferredInitializer<Collection<AnnotationInstance>>() {
				@Override
				public Collection<AnnotationInstance> initialize() {
					List<AnnotationInstance> annotations = new ArrayList<AnnotationInstance>();
					annotations.addAll( resolveOrEmpty( HibernateDotNames.GENERIC_GENERATOR ) );
					for ( AnnotationInstance generatorsAnnotation : resolveOrEmpty( HibernateDotNames.GENERIC_GENERATORS ) ) {
						Collections.addAll(
								annotations,
								JandexHelper.getValue(
										generatorsAnnotation,
										"value",
										AnnotationInstance[].class,
										classLoaderService
								)
						);
					}
					return annotations;
				}
			}
	);


	@Override
	public Collection<AnnotationInstance> getGenericGeneratorSources() {
		return genericGeneratorSources.getValue();
	}

	protected abstract Collection<AnnotationInstance> getAnnotations(DotName name);
}
