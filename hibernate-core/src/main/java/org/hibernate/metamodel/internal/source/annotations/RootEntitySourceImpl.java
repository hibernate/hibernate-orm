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
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.entity.IdType;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.IdGenerator;
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

/**
 * @author Hardy Ferentschik
 */
public class RootEntitySourceImpl extends EntitySourceImpl implements RootEntitySource {
	private final RootEntityClass rootEntityClass;

	public RootEntitySourceImpl(RootEntityClass entityClass) {
		super( entityClass );
		rootEntityClass = entityClass;
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		IdType idType = rootEntityClass.getIdType();
		switch ( idType ) {
			case SIMPLE: {
				BasicAttribute attribute = getEntityClass().getIdAttributes().iterator().next();
				return new SimpleIdentifierSourceImpl(
						attribute,
						getEntityClass().getAttributeOverrideMap().get(attribute.getName())
				);
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
			Iterator<BasicAttribute> idAttributes = rootEntitySource.getEntityClass().getIdAttributes().iterator();
			if ( !idAttributes.hasNext() ) {
				throw rootEntitySource.getLocalBindingContext().makeMappingException(
						String.format(
								"Could not locate identifier attributes on entity %s",
								rootEntitySource.getEntityName()
						)
				);
			}
			final BasicAttribute idAttribute = idAttributes.next();
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

			// todo : no idea how to obtain overrides here...
			Map<String, AttributeOverride> overrides = getEntityClass().getAttributeOverrideMap();
			componentAttributeSource = new ComponentAttributeSourceImpl( embeddableClass, "", overrides );
		}

		@Override
		public ComponentAttributeSource getIdentifierAttributeSource() {
			return componentAttributeSource;
		}

		@Override
		public IdGenerator getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdGenerator getIdentifierGeneratorDescriptor() {
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
			final AnnotationInstance idClassAnnotation = JandexHelper.getSingleAnnotation(
					rootEntitySource.getEntityClass().getClassInfo(),
					JPADotNames.ID_CLASS
			);
			if ( idClassAnnotation == null ) {
				return null;
			}

			return rootEntitySource.getLocalBindingContext().locateClassByName(
					JandexHelper.getValue( idClassAnnotation, "value", String.class )
			);
		}

		@Override
		public String getIdClassPropertyAccessorName() {
			// TODO: retrieve property accessor name for IdClass
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
			List<SingularAttributeSource> attributeSources = new ArrayList<SingularAttributeSource>();
			for ( BasicAttribute attr : rootEntitySource.getEntityClass().getIdAttributes() ) {
				attributeSources.add( new SingularAttributeSourceImpl( attr ) );
			}
			return attributeSources;
		}

		@Override
		public IdGenerator getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdGenerator getIdentifierGeneratorDescriptor() {
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


