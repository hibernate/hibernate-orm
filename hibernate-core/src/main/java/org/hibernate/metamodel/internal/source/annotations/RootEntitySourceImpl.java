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

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.entity.IdType;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.RootEntitySource;
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
				return new SimpleIdentifierSourceImpl( this, (BasicAttribute) attribute );
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
	public VersionAttributeSource getVersionAttributeSource() {
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

	Class locateIdClassType() {
		final RootEntityClass rootEntityClass = (RootEntityClass) getEntityClass();
		final AnnotationInstance idClassAnnotation = rootEntityClass.getIdClassAnnotation();

		if ( idClassAnnotation == null ) {
			return null;
		}

		return getLocalBindingContext().locateClassByName(
				JandexHelper.getValue(
						idClassAnnotation,
						"value",
						String.class,
						rootEntityClass.getLocalBindingContext().getBuildingOptions()
								.getServiceRegistry()
								.getService( ClassLoaderService.class )
				)
		);
	}

	String determineIdClassAccessStrategy() {
		return getEntityClass().getClassAccessType().name().toLowerCase();
	}

}


