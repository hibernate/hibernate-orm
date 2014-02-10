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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
* @author Steve Ebersole
*/
class NonAggregatedCompositeIdentifierSourceImpl implements NonAggregatedCompositeIdentifierSource {
	private final RootEntitySourceImpl rootEntitySource;

	public NonAggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;
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
	public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
		List<SingularAttributeSource> attributeSources = new ArrayList<SingularAttributeSource>();
		for ( MappedAttribute attr : rootEntitySource.getEntityClass().getIdAttributes().values() ) {
			switch ( attr.getNature() ) {
				case BASIC:
					attributeSources.add( new SingularAttributeSourceImpl( attr ) );
					break;
				case MANY_TO_ONE:
				case ONE_TO_ONE:
					final SingularAssociationAttribute associationAttribute = (SingularAssociationAttribute) attr;
					final SingularAttributeSource attributeSource;
					if ( associationAttribute.getMappedBy() == null ) {
						final RootEntityClass rootEntityClass = (RootEntityClass) rootEntitySource.getEntityClass();
						attributeSource = new ToOneAttributeSourceImpl(
								associationAttribute,
								"",
								rootEntityClass.getLocalBindingContext()
						);
					}
					else {
						attributeSource = new ToOneMappedByAttributeSourceImpl( associationAttribute, "" );
					}
					attributeSources.add( attributeSource );
			}
		}
		return attributeSources;
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
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}
}
