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
package org.hibernate.metamodel.binder.source.annotations;

import javax.persistence.SharedCacheMode;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.binder.source.RootEntityDescriptor;
import org.hibernate.metamodel.binder.source.TableDescriptor;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.InheritanceType;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Hardy Ferentschik
 */
public class RootEntityDescriptorImpl extends AbstractEntityDescriptorImpl implements RootEntityDescriptor {
	private final boolean mutable;
	private final boolean explicitPolymorphism;
	private final String whereFilter;
	private final String rowId;
	private final Caching caching;
	private final OptimisticLockStyle optimisticLockStyle;

	private final TableDescriptor baseTableDescriptor;

	public RootEntityDescriptorImpl(ConfiguredClass configuredClass, AnnotationsBindingContext bindingContext) {
		super( configuredClass, null, InheritanceType.NO_INHERITANCE, bindingContext );

		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		OptimisticLockType optimisticLockType = OptimisticLockType.VERSION;
		if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
			optimisticLockType = OptimisticLockType.valueOf( hibernateEntityAnnotation.value( "optimisticLock" ).asEnum() );
		}
		this.optimisticLockStyle = OptimisticLockStyle.valueOf( optimisticLockType.name() );

		final PolymorphismType polymorphism = hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "polymorphism" ) != null
				? PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() )
				: PolymorphismType.IMPLICIT;
		this.explicitPolymorphism = polymorphism != PolymorphismType.IMPLICIT;

		final AnnotationInstance hibernateImmutableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		this.mutable = hibernateImmutableAnnotation == null
				&& hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "mutable" ) != null
				&& hibernateEntityAnnotation.value( "mutable" ).asBoolean();

		final AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.WHERE
		);
		this.whereFilter = whereAnnotation != null && whereAnnotation.value( "clause" ) != null
				? whereAnnotation.value( "clause" ).asString()
				: null;

		final AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ROW_ID
		);
		this.rowId = rowIdAnnotation != null && rowIdAnnotation.value() != null
				? rowIdAnnotation.value().asString()
				: null;

		this.caching = interpretCaching( configuredClass, bindingContext );

		final AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.TABLE
		);
		this.baseTableDescriptor = new TableDescriptorImpl(
				tableAnnotation.value( "schema" ) == null
						? null
						: tableAnnotation.value( "schema" ).asString(),
				tableAnnotation.value( "catalog" ) == null
						? null
						: tableAnnotation.value( "catalog" ).asString(),
				tableAnnotation.value( "name" ) == null
						? null
						: tableAnnotation.value( "name" ).asString(),
				this,
				bindingContext
		);
	}

	private Caching interpretCaching(ConfiguredClass configuredClass, AnnotationsBindingContext bindingContext) {
		final AnnotationInstance hibernateCacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( hibernateCacheAnnotation != null ) {
			final AccessType accessType = hibernateCacheAnnotation.value( "usage" ) == null
					? bindingContext.getMappingDefaults().getCacheAccessType()
					: CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() ).toAccessType();
			return new Caching(
					hibernateCacheAnnotation.value( "region" ) == null
							? configuredClass.getName()
							: hibernateCacheAnnotation.value( "region" ).asString(),
					accessType,
					hibernateCacheAnnotation.value( "include" ) != null
							&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
			);
		}

		final AnnotationInstance jpaCacheableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( jpaCacheableAnnotation != null && jpaCacheableAnnotation.value() != null ) {
			cacheable = jpaCacheableAnnotation.value().asBoolean();
		}

		final boolean doCaching;
		switch ( bindingContext.getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				doCaching = true;
				break;
			}
			case ENABLE_SELECTIVE: {
				doCaching = cacheable;
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null || cacheable;
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				doCaching = false;
				break;
			}
		}

		if ( ! doCaching ) {
			return null;
		}

		return new Caching(
				configuredClass.getName(),
				bindingContext.getMappingDefaults().getCacheAccessType(),
				true
		);
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	@Override
	public String getWhereFilter() {
		return whereFilter;
	}

	@Override
	public String getRowId() {
		return rowId;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public TableDescriptor getBaseTable() {
		return baseTableDescriptor;
	}
}
