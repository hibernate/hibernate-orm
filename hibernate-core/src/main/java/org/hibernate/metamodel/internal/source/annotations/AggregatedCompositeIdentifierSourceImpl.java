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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ToolingHintSource;

/**
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
class AggregatedCompositeIdentifierSourceImpl implements AggregatedCompositeIdentifierSource {
	private final RootEntitySourceImpl rootEntitySource;
	private final ComponentAttributeSourceImpl componentAttributeSource;

	public AggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;
		// the entity class reference should contain one single id attribute...
		Iterator<MappedAttribute> idAttributes = rootEntitySource.getEntityClass().getIdAttributes().values().iterator();
		noIdentifierCheck( rootEntitySource, idAttributes );
		final MappedAttribute idAttribute = idAttributes.next();
		if ( idAttributes.hasNext() ) {
			throw rootEntitySource.getLocalBindingContext().makeMappingException(
					String.format(
							"Encountered multiple identifier attributes on entity %s",
							rootEntitySource.getEntityName()
					)
			);
		}

		final EmbeddableClass embeddableClass = rootEntitySource.getEntityClass().getEmbeddedClasses().get(
				idAttribute.getName()
		);
		if ( embeddableClass == null ) {
			throw rootEntitySource.getLocalBindingContext().makeMappingException(
					"Could not locate embedded identifier class metadata"
			);
		}

		componentAttributeSource = new ComponentAttributeSourceImpl( embeddableClass, "", embeddableClass.getClassAccessType() );
	}

	private void noIdentifierCheck(RootEntitySourceImpl rootEntitySource, Iterator<MappedAttribute> idAttributes) {
		if ( !idAttributes.hasNext() ) {
			throw rootEntitySource.getLocalBindingContext().makeMappingException(
					String.format(
							"Could not locate identifier attributes on entity %s",
							rootEntitySource.getEntityName()
					)
			);
		}
	}

	@Override
	public Class getLookupIdClass() {
		return rootEntitySource.locateIdClassType();
	}

	@Override
	public String getIdClassPropertyAccessorName() {
		return rootEntitySource.determineIdClassAccessStrategy();
	}

	@Override
	public ComponentAttributeSource getIdentifierAttributeSource() {
		return componentAttributeSource;
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
		return EntityIdentifierNature.AGGREGATED_COMPOSITE;
	}

	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}
}
