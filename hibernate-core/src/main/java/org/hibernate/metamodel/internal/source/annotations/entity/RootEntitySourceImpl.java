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

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.DiscriminatorSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.attribute.SimpleIdentifierSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAttributeSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.attribute.VersionAttributeSourceImpl;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class RootEntitySourceImpl extends EntitySourceImpl implements RootEntitySource {
	public RootEntitySourceImpl(EntityClass entityClass) {
		super( entityClass );
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		IdType idType = getEntityClass().getIdType();
		switch ( idType ) {
			case SIMPLE: {
				BasicAttribute attribute = getEntityClass().getIdAttributes().iterator().next();
				return new SimpleIdentifierSourceImpl( attribute, getEntityClass().getAttributeOverrideMap() );
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
		if ( getEntityClass().getDiscriminatorColumnValues() != null ) {
			discriminatorSource = new DiscriminatorSourceImpl( getEntityClass() );
		}
		return discriminatorSource;
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


	private class AggregatedCompositeIdentifierSourceImpl implements AggregatedCompositeIdentifierSource {
		private final ComponentAttributeSourceImpl componentAttributeSource;

		public AggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
			componentAttributeSource = null;
			throw new NotYetImplementedException( "Not really yet implemented because we cannot find the component attribute defining the id yet" );
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
		public Nature getNature() {
			return Nature.AGGREGATED_COMPOSITE;
		}

		@Override
		public String getUnsavedValue() {
			return null;
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
		public Nature getNature() {
			return Nature.COMPOSITE;
		}

		@Override
		public String getUnsavedValue() {
			return null;
		}
	}



}


