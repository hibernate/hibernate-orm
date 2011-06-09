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
package org.hibernate.metamodel.source.annotations.entity.state.binding;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.source.spi.BindingContext;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Gail Badner
 */
public class EntityBindingStateImpl implements EntityBindingState {
	private final String entityName;
	private final boolean isRoot;
	private final InheritanceType inheritanceType;
	private final Caching caching;
	private final MetaAttributeContext metaAttributeContext;

	private final boolean mutable;
	private final boolean explicitPolymorphism;
	private final String whereFilter;
	private final String rowId;

	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;

	private final int batchSize;
	private final boolean selectBeforeUpdate;
	private final OptimisticLockType optimisticLock;

	private final Class persisterClass;
	private final Boolean isAbstract;

	private final boolean lazy;
	private final String proxyInterfaceName;

	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;

	private final List<String> synchronizedTableNames;

	public EntityBindingStateImpl(BindingContext bindingContext, ConfiguredClass configuredClass, String entityName) {
		this.entityName = entityName;
		this.isRoot = configuredClass.isRoot();
		this.inheritanceType = configuredClass.getInheritanceType();

		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ENTITY
		);
		AnnotationInstance immutableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		this.mutable = immutableAnnotation == null && getBooleanValue( hibernateEntityAnnotation, "mutable", true );
		this.dynamicInsert = getBooleanValue( hibernateEntityAnnotation, "dynamicInsert",  false );
		this.dynamicUpdate = getBooleanValue( hibernateEntityAnnotation, "dynamicUpdate",  false );
		this.selectBeforeUpdate = getBooleanValue( hibernateEntityAnnotation, "selectBeforeUpdate", false );

		PolymorphismType polymorphism =
				PolymorphismType.valueOf(
						getEnumStringValue(
								hibernateEntityAnnotation, "polymorphism", PolymorphismType.IMPLICIT.name()
						)
				);
		this.explicitPolymorphism = PolymorphismType.EXPLICIT.equals( polymorphism );

		this.optimisticLock =
				OptimisticLockType.valueOf(
						getEnumStringValue(
								hibernateEntityAnnotation, "optimisticLock", OptimisticLockType.VERSION.name()
						)
				);

		String persisterClassName = getStringValue( hibernateEntityAnnotation, "persister", null );
		this.persisterClass = (
				persisterClassName == null ?
						null :
						bindingContext
								.getServiceRegistry()
								.getService( ClassLoaderService.class )
								.classForName( persisterClassName )
		);

		this.whereFilter = getWhereFilter( configuredClass );

		Caching hibernateCaching = getHibernateCaching( configuredClass, entityName );
		this.caching = hibernateCaching != null ?
				hibernateCaching :
				getJpaCaching( bindingContext, configuredClass, entityName );

		AnnotationInstance proxyAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.PROXY
		);
		this.lazy = getBooleanValue( proxyAnnotation, "lazy", true );
		this.proxyInterfaceName = getStringValue( proxyAnnotation, "proxyClass", null );

		// TODO: where do these values come from?
		this.metaAttributeContext = null;
		this.rowId = null;
		this.batchSize = -1;
		this.isAbstract = false;
		this.customInsert = null;
		this.customUpdate = null;
		this.customDelete = null;
		this.synchronizedTableNames = null;
	}

 	private static String getWhereFilter(ConfiguredClass configuredClass) {
		 AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				 configuredClass.getClassInfo(), HibernateDotNames.WHERE
		 );
		 return getStringValue( whereAnnotation, "clause", null );
	 }

	// This does not take care of any inheritance of @Cacheable within a class hierarchy as specified in JPA2.
	// This is currently not supported (HF)
	private static Caching getJpaCaching(BindingContext bindingContext, ConfiguredClass configuredClass, String entityName) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( cacheAnnotation != null && cacheAnnotation.value() != null ) {
			cacheable = cacheAnnotation.value().asBoolean();
		}

		Caching caching = null;
		switch ( bindingContext.getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				caching = createCachingForCacheableAnnotation( bindingContext, entityName );
				break;
			}
			case ENABLE_SELECTIVE: {
				if ( cacheable ) {
					caching = createCachingForCacheableAnnotation(  bindingContext, entityName );
				}
				break;
			}
			case DISABLE_SELECTIVE: {
				if ( cacheAnnotation == null || cacheable ) {
					caching = createCachingForCacheableAnnotation( bindingContext, entityName );
				}
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				break;
			}
		}
		return caching;
	}

	private static Caching createCachingForCacheableAnnotation(BindingContext bindingContext, String region) {
		RegionFactory regionFactory = bindingContext.getServiceRegistry().getService( RegionFactory.class );
		AccessType defaultAccessType = regionFactory.getDefaultAccessType();
		return new Caching( region, defaultAccessType, true );
	}

	private static Caching getHibernateCaching(ConfiguredClass configuredClass, String entityName) {
		AnnotationInstance cacheAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CACHE
		);
		if ( cacheAnnotation == null ) {
			return null;
		}

		String region;
		if ( cacheAnnotation.value( "region" ) != null ) {
			region = cacheAnnotation.value( "region" ).asString();
		}
		else {
			region = entityName;
		}

		boolean cacheLazyProperties = true;
		if ( cacheAnnotation.value( "include" ) != null ) {
			String tmp = cacheAnnotation.value( "include" ).asString();
			if ( "all".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = true;
			}
			else if ( "non-lazy".equalsIgnoreCase( tmp ) ) {
				cacheLazyProperties = false;
			}
			else {
				throw new AnnotationException( "Unknown lazy property annotations: " + tmp );
			}
		}

		CacheConcurrencyStrategy strategy = CacheConcurrencyStrategy.valueOf(
				cacheAnnotation.value( "usage" ).asEnum()
		);
		return new Caching( region, strategy.toAccessType(), cacheLazyProperties );
	}

	private static String getStringValue(
			AnnotationInstance annotationInstance,
			String name,
			String defaultValue) {
		AnnotationValue value = getAnnotationValue( annotationInstance, name );
		return value == null ? defaultValue : value.asString();
	}

	private static boolean getBooleanValue(
			AnnotationInstance annotationInstance,
			String name,
			boolean defaultValue) {
		AnnotationValue value = getAnnotationValue( annotationInstance, name );
		return value == null ? defaultValue : value.asBoolean();
	}

	private static String getEnumStringValue(
			AnnotationInstance annotationInstance,
			String name,
			String defaultValue) {
		AnnotationValue value = getAnnotationValue( annotationInstance, name );
		return value == null ? defaultValue : value.asEnum();
	}

	private static AnnotationValue getAnnotationValue(
			AnnotationInstance annotationInstance,
			String name) {
		return annotationInstance == null ? null : annotationInstance.value( name );
	}

	@Override
	public boolean isRoot() {
		return isRoot;

	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return inheritanceType;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	@Override
	public boolean isLazy() {
		return lazy;
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
	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	@Override
	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	@Override
	public int getOptimisticLockMode() {
		return optimisticLock.ordinal();
	}

	@Override
	public Class getEntityPersisterClass() {
		return persisterClass;
	}

	@Override
	public Boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	@Override
	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	@Override
	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}
}
