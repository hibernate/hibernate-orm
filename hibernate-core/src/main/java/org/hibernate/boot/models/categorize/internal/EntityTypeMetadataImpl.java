/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizationContext;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.CustomSqlMapping;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;

/// Standard EntityTypeMetadata impl
///
/// @since 9.0
/// @author Steve Ebersole
public class EntityTypeMetadataImpl
		extends AbstractIdentifiableTypeMetadata
		implements EntityTypeMetadata, EntityNaming {
	private final String entityName;
	private final String jpaEntityName;

	private final List<AttributeMetadata> attributeList;

	private final boolean mutable;
	private final boolean cacheable;
	private final boolean isLazy;
	private final String proxy;
	private final int batchSize;
	private final String discriminatorMatchValue;
	private final boolean isDynamicInsert;
	private final boolean isDynamicUpdate;
	private final CustomSqlMapping customInsert;
	private final CustomSqlMapping customUpdate;
	private final CustomSqlMapping customDelete;
	private final String[] synchronizedTableNames;

	private List<JpaEventListener> hierarchyEventListeners;
	private List<JpaEventListener> completeEventListeners;

	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			ManagedTypeInheritanceState inheritanceState,
			HierarchyMetadataCollector metadataCollector,
			CategorizationContext modelContext) {
		super( classDetails, hierarchy, inheritanceState, modelContext );

		// NOTE: There is no annotation for `entity-name` - it comes exclusively from XML
		// 		mappings.  By default, the `entityName` is simply the entity class name.
		// 		`ClassDetails#getName` already handles this all for us
		this.entityName = getClassDetails().getName();

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsert = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLInsert.class ) );
		this.customUpdate = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLUpdate.class ) );
		this.customDelete = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLDelete.class ) );

		this.isLazy = true;
		this.proxy = getEntityName();

		final DiscriminatorValue discriminatorValueAnn = classDetails.getDirectAnnotationUsage( DiscriminatorValue.class );
		this.discriminatorMatchValue = discriminatorValueAnn == null ? null : discriminatorValueAnn.value();

		postInstantiate( metadataCollector );
	}

	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			ManagedTypeInheritanceState inheritanceState,
			HierarchyMetadataCollector metadataCollector,
			CategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, inheritanceState, modelContext );

		// NOTE: There is no annotation for `entity-name` - it comes exclusively from XML
		// 		mappings.  By default, the `entityName` is simply the entity class name.
		// 		`ClassDetails#getName` already handles this all for us
		this.entityName = getClassDetails().getName();

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsert = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLInsert.class ) );
		this.customUpdate = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLUpdate.class ) );
		this.customDelete = extractCustomSql( classDetails.getDirectAnnotationUsage( SQLDelete.class ) );

		this.isLazy = true;
		this.proxy = getEntityName();

		final DiscriminatorValue discriminatorValueAnn = classDetails.getDirectAnnotationUsage( DiscriminatorValue.class );
		this.discriminatorMatchValue = discriminatorValueAnn == null ? null : discriminatorValueAnn.value();

		postInstantiate( metadataCollector );
	}

	@Override
	protected List<AttributeMetadata> attributeList() {
		return attributeList;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public String getClassName() {
		return getClassDetails().getClassName();
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isDynamicInsert() {
		return isDynamicInsert;
	}

	@Override
	public boolean isDynamicUpdate() {
		return isDynamicUpdate;
	}

	@Override
	public CustomSqlMapping getCustomInsert() {
		return customInsert;
	}

	@Override
	public CustomSqlMapping getCustomUpdate() {
		return customUpdate;
	}

	@Override
	public CustomSqlMapping getCustomDelete() {
		return customDelete;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public String getProxy() {
		return proxy;
	}

	@Override
	public List<JpaEventListener> getHierarchyJpaEventListeners() {
		return hierarchyEventListeners;
	}

	@Override
	public List<JpaEventListener> getCompleteJpaEventListeners() {
		return completeEventListeners;
	}


	private String determineJpaEntityName(Entity entityAnnotation, String entityName) {
		final String name = entityAnnotation.name();
		if ( isNotEmpty( name ) ) {
			return name;
		}
		return unqualify( entityName );
	}

	private boolean determineMutability(ClassDetails classDetails, CategorizationContext modelContext) {
		final Immutable immutableAnn = classDetails.getDirectAnnotationUsage( Immutable.class );
		return immutableAnn == null;
	}

	private boolean determineCacheability(
			ClassDetails classDetails,
			CategorizationContext modelContext) {
		final Cacheable cacheableAnn = classDetails.getDirectAnnotationUsage( Cacheable.class );
		switch ( modelContext.getSharedCacheMode() ) {
			case NONE: {
				return false;
			}
			case ALL: {
				return true;
			}
			case DISABLE_SELECTIVE: {
				// Disable caching for all `@Cacheable(false)`, enabled otherwise (including no annotation)
				//noinspection RedundantIfStatement
				if ( cacheableAnn == null || cacheableAnn.value() ) {
					// not disabled
					return true;
				}
				else {
					// disable, there was an explicit `@Cacheable(false)`
					return false;
				}
			}
			default: {
				// ENABLE_SELECTIVE
				// UNSPECIFIED

				// Enable caching for all `@Cacheable(true)`, disable otherwise (including no annotation)
				//noinspection RedundantIfStatement
				if ( cacheableAnn != null && cacheableAnn.value() ) {
					// enable, there was an explicit `@Cacheable(true)`
					return true;
				}
				else {
					return false;
				}
			}
		}
	}

	/**
	 * Build custom SQL mutation details from {@link org.hibernate.annotations.SQLInsert},
	 * {@link org.hibernate.annotations.SQLUpdate}, {@link org.hibernate.annotations.SQLDelete}
	 * or {@link org.hibernate.annotations.SQLDeleteAll} annotations
	 */
	public static CustomSqlMapping extractCustomSql(SQLInsert customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		return new CustomSqlMapping(
				customSqlAnnotation.sql(),
				customSqlAnnotation.callable(),
				determineExpectation( customSqlAnnotation.verify() )
		);
	}

	public static CustomSqlMapping extractCustomSql(SQLUpdate customSqlAnnotation) {
		return customSqlAnnotation == null
				? null
				: new CustomSqlMapping(
						customSqlAnnotation.sql(),
						customSqlAnnotation.callable(),
						determineExpectation( customSqlAnnotation.verify() )
				);
	}

	public static CustomSqlMapping extractCustomSql(SQLDelete customSqlAnnotation) {
		return customSqlAnnotation == null
				? null
				: new CustomSqlMapping(
						customSqlAnnotation.sql(),
						customSqlAnnotation.callable(),
						determineExpectation( customSqlAnnotation.verify() )
				);
	}

	private static java.util.function.Supplier<? extends Expectation> determineExpectation(
			Class<? extends Expectation> expectationClass) {
		return expectationClass == Expectation.class ? null : getDefaultSupplier( expectationClass );
	}

	private String[] determineSynchronizedTableNames() {
		final Synchronize synchronizeAnnotation = getClassDetails().getDirectAnnotationUsage( Synchronize.class );
		if ( synchronizeAnnotation != null ) {
			return synchronizeAnnotation.value();
		}
		return EMPTY_STRINGS;
	}

	private int determineBatchSize() {
		final BatchSize batchSizeAnnotation = getClassDetails().getDirectAnnotationUsage( BatchSize.class );
		if ( batchSizeAnnotation != null ) {
			return batchSizeAnnotation.size();
		}
		return -1;
	}

	private boolean decodeDynamicInsert() {
		final DynamicInsert dynamicInsertAnnotation = getClassDetails().getDirectAnnotationUsage( DynamicInsert.class );
		if ( dynamicInsertAnnotation == null ) {
			return false;
		}

		return true;
	}

	private boolean decodeDynamicUpdate() {
		final DynamicUpdate dynamicUpdateAnnotation = getClassDetails().getDirectAnnotationUsage( DynamicUpdate.class );
		if ( dynamicUpdateAnnotation == null ) {
			return false;
		}
		return true;
	}
}
