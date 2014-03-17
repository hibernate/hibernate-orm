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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifierSource implements IdentifierSource {
	private final RootEntitySourceImpl rootEntitySource;
	private final Class lookupIdClass;

	protected AbstractIdentifierSource(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;

		this.lookupIdClass = resolveLookupIdClass( rootEntitySource );
	}

	private Class resolveLookupIdClass(RootEntitySourceImpl rootEntitySource) {
		final AnnotationInstance idClassAnnotation = rootEntitySource.getEntityClass()
				.getJavaTypeDescriptor()
				.findTypeAnnotation( JPADotNames.ID_CLASS );
		if ( idClassAnnotation == null ) {
			return null;
		}

		final AnnotationValue idClassValue = idClassAnnotation.value();
		if ( idClassValue == null ) {
			return null;
		}

		final String idClassName = StringHelper.nullIfEmpty( idClassValue.asString() );
		if ( idClassName == null ) {
			return null;
		}

		return rootEntitySource.getLocalBindingContext().getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( idClassName );
	}

	protected RootEntitySourceImpl rootEntitySource() {
		return rootEntitySource;
	}

	@Override
	public Class getLookupIdClass() {
		return lookupIdClass;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return Collections.emptySet();
	}
}
