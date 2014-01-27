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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.entity.IdType;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class RootEntitySourceImpl extends EntitySourceImpl implements RootEntitySource {
	private final RootEntityClass rootEntityClass;

	public RootEntitySourceImpl(RootEntityClass entityClass) {
		super( entityClass );
		this.rootEntityClass = entityClass;
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		IdType idType = rootEntityClass.getIdType();
		switch ( idType ) {
			case SIMPLE: {
				MappedAttribute attribute = getEntityClass().getIdAttributes().values().iterator().next();
				return new SimpleIdentifierSourceImpl(rootEntityClass, (BasicAttribute) attribute);
			}
			case COMPOSED: {
				return new NonAggregatedCompositeIdentifierSourceImpl( this );
			}
			case EMBEDDED: {
				return new AggregatedCompositeIdentifierSourceImpl( this );
			}
			default: {
				throw new AssertionFailure(
						String.format( "Entity [%s] did not define an identifier", getEntityName() )
				);
			}
		}
	}

	@Override
	public VersionAttributeSource getVersioningAttributeSource() {
		final EntityClass entityClass = getEntityClass();
		if ( entityClass.getVersionAttribute() == null ) {
			return null;
		}
		return new VersionAttributeSourceImpl( entityClass.getVersionAttribute() );
	}

	@Override
	public DiscriminatorSource getDiscriminatorSource() {
		DiscriminatorSource discriminatorSource = null;
		if ( rootEntityClass.needsDiscriminatorColumn() ) {
			discriminatorSource = new DiscriminatorSourceImpl( rootEntityClass );
		}
		return discriminatorSource;
	}

	@Override
	public MultiTenancySource getMultiTenancySource() {
		return getEntityClass().hasMultiTenancySourceInformation()
				? new MutliTenancySourceImpl( getEntityClass() )
				: null;
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public boolean isMutable() {
		return getEntityClass().isMutable();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return getEntityClass().isExplicitPolymorphism();
	}

	@Override
	public String getWhere() {
		return getEntityClass().getWhereClause();
	}

	@Override
	public String getRowId() {
		return getEntityClass().getRowId();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return getEntityClass().getOptimisticLockStyle();
	}

	@Override
	public Caching getCaching() {
		return getEntityClass().getCaching();
	}

	@Override
	public Caching getNaturalIdCaching() {
		return getEntityClass().getNaturalIdCaching();
	}

	private class AggregatedCompositeIdentifierSourceImpl implements AggregatedCompositeIdentifierSource {
		private final ComponentAttributeSourceImpl componentAttributeSource;

		public AggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
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
		public Iterable<MetaAttributeSource> getMetaAttributeSources() {
			// not relevant for annotations
			return Collections.emptySet();
		}
	}

	private class NonAggregatedCompositeIdentifierSourceImpl implements NonAggregatedCompositeIdentifierSource {
		private final RootEntitySourceImpl rootEntitySource;

		public NonAggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
			this.rootEntitySource = rootEntitySource;
		}

		@Override
		public Class getLookupIdClass() {
			final AnnotationInstance idClassAnnotation = ( 
					( RootEntityClass ) rootEntitySource.getEntityClass() )
							.getIdClassAnnotation();
			
			if ( idClassAnnotation == null ) {
				return null;
			}

			return rootEntitySource.getLocalBindingContext().locateClassByName(
					JandexHelper.getValue( idClassAnnotation, "value", String.class,
							rootEntityClass.getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) )
			);
		}

		@Override
		public String getIdClassPropertyAccessorName() {
			// TODO: Should we retrieve property accessor name for the ID Class?
			return rootEntitySource.getEntityClass().getClassAccessType().name()
					.toLowerCase();
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
						final SingularAttributeSource attributeSource =
								associationAttribute.getMappedBy() == null ?
										new ToOneAttributeSourceImpl( associationAttribute, "",
												rootEntityClass.getLocalBindingContext() ) :
										new ToOneMappedByAttributeSourceImpl( associationAttribute, "" );
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
}


