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
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
* @author Steve Ebersole
*/
class NonAggregatedCompositeIdentifierSourceImpl
		extends AbstractIdentifierSource
		implements NonAggregatedCompositeIdentifierSource {

	private final List<SingularAttributeSource> idAttributes;
	private final Class lookupIdClass;

	public NonAggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
		super( rootEntitySource );

		this.idAttributes = rootEntitySource.getIdentifierAttributes();

		final JavaTypeDescriptor idClassDescriptor = resolveIdClassDescriptor();
		if ( idClassDescriptor == null ) {
			// probably this is an error...
			this.lookupIdClass = null;
		}
		else {
			final ClassLoaderService cls = rootEntitySource.getLocalBindingContext()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			this.lookupIdClass = cls.classForName( idClassDescriptor.getName().toString() );
		}
	}

	private JavaTypeDescriptor resolveIdClassDescriptor() {
		final AnnotationInstance idClassAnnotation = rootEntitySource().getEntityClass()
				.getJavaTypeDescriptor()
				.findTypeAnnotation( JPADotNames.ID_CLASS );

		if ( idClassAnnotation == null ) {
			return null;
		}

		if ( idClassAnnotation.value() == null ) {
			return null;
		}

		return rootEntitySource().getLocalBindingContext().getJavaTypeDescriptorRepository().getType(
				DotName.createSimple( idClassAnnotation.value().asString() )
		);
	}

	@Override
	public Class getLookupIdClass() {
		return lookupIdClass;
	}

	@Override
	public String getIdClassPropertyAccessorName() {
		return idAttributes.get( 0 ).getPropertyAccessorName();
	}

	@Override
	public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
		// for now, return null.  this is that stupid specj bs
		return null;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		// annotations do not currently allow generators to be attached to composite identifiers as a whole
		return null;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
	}

	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		// todo : however, it is relevant for mocked annotations comeing from XML!!!
		return Collections.emptySet();
	}

	@Override
	public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
		return idAttributes;
	}
}
