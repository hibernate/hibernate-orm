/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * @author Steve Ebersole
 */
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
	private final Map<String,CustomSql> customInsertMap;
	private final Map<String,CustomSql>  customUpdateMap;
	private final Map<String,CustomSql>  customDeleteMap;
	private final String[] synchronizedTableNames;

	private List<JpaEventListener> hierarchyEventListeners;
	private List<JpaEventListener> completeEventListeners;

	/**
	 * Form used when the entity is the absolute-root (no mapped-super) of the hierarchy
	 */
	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AccessType defaultAccessType,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, null, defaultAccessType, modelContext );

		// NOTE: There is no annotation for `entity-name` - it comes exclusively from XML
		// 		mappings.  By default, the `entityName` is simply the entity class name.
		// 		`ClassDetails#getName` already handles this all for us
		this.entityName = getClassDetails().getName();

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails, modelContext );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsertMap = extractCustomSql( classDetails, SQLInsert.class );
		this.customUpdateMap = extractCustomSql( classDetails, SQLUpdate.class );
		this.customDeleteMap = extractCustomSql( classDetails, SQLDelete.class );

		// defaults are that it is lazy and that the class itself is the proxy class
		this.isLazy = true;
		this.proxy = getEntityName();

		final DiscriminatorValue discriminatorValueAnn = classDetails.getDirectAnnotationUsage( DiscriminatorValue.class );
		if ( discriminatorValueAnn != null ) {
			this.discriminatorMatchValue = discriminatorValueAnn.value();
		}
		else {
			this.discriminatorMatchValue = null;
		}

		postInstantiate( true, typeConsumer );
	}

	/**
	 * Form used when the entity is NOT the absolute-root of the hierarchy
	 */
	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			HierarchyTypeConsumer typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, modelContext );

		// NOTE: There is no annotation for `entity-name` - it comes exclusively from XML
		// 		mappings.  By default, the `entityName` is simply the entity class name.
		// 		`ClassDetails#getName` already handles this all for us
		this.entityName = getClassDetails().getName();

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( classDetails, modelContext );
		this.attributeList = resolveAttributes( lifecycleCallbackCollector );
		this.hierarchyEventListeners = collectHierarchyEventListeners( lifecycleCallbackCollector.resolve() );
		this.completeEventListeners = collectCompleteEventListeners( modelContext );

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsertMap = extractCustomSql( classDetails, SQLInsert.class );
		this.customUpdateMap = extractCustomSql( classDetails, SQLUpdate.class );
		this.customDeleteMap = extractCustomSql( classDetails, SQLDelete.class );

		// defaults are that it is lazy and that the class itself is the proxy class
		this.isLazy = true;
		this.proxy = getEntityName();

		final DiscriminatorValue discriminatorValueAnn = classDetails.getDirectAnnotationUsage( DiscriminatorValue.class );
		if ( discriminatorValueAnn != null ) {
			this.discriminatorMatchValue = discriminatorValueAnn.value();
		}
		else {
			this.discriminatorMatchValue = null;
		}

		postInstantiate( true, typeConsumer );
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
	public Map<String, CustomSql> getCustomInserts() {
		return customInsertMap;
	}

	@Override
	public Map<String, CustomSql> getCustomUpdates() {
		return customUpdateMap;
	}

	@Override
	public Map<String, CustomSql> getCustomDeletes() {
		return customDeleteMap;
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

	private boolean determineMutability(ClassDetails classDetails, ModelCategorizationContext modelContext) {
		final Immutable immutableAnn = classDetails.getDirectAnnotationUsage( Immutable.class );
		return immutableAnn == null;
	}

	private boolean determineCacheability(
			ClassDetails classDetails,
			ModelCategorizationContext modelContext) {
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
	 * Build a CustomSql reference from {@link SQLInsert},
	 * {@link SQLUpdate}, {@link SQLDelete}
	 * or {@link org.hibernate.annotations.SQLDeleteAll} annotations
	 */
	public static <A extends Annotation> Map<String,CustomSql> extractCustomSql(ClassDetails classDetails, Class<A> annotationType) {
		final A[] annotationUsages = classDetails.getRepeatedAnnotationUsages( annotationType, null );
		if ( CollectionHelper.isEmpty( annotationUsages ) ) {
			return Collections.emptyMap();
		}

		final Map<String, CustomSql> result = new HashMap<>();
		ArrayHelper.forEach( annotationUsages, (customSqlAnnotation) -> {
			final CustomSqlDetails customSqlDetails = (CustomSqlDetails) customSqlAnnotation;
			final String sql = customSqlDetails.sql();
			final boolean isCallable = customSqlDetails.callable();

			final ResultCheckStyle checkValue = customSqlDetails.check();
			final ExecuteUpdateResultCheckStyle checkStyle;
			if ( checkValue == null ) {
				checkStyle = isCallable
						? ExecuteUpdateResultCheckStyle.NONE
						: ExecuteUpdateResultCheckStyle.COUNT;
			}
			else {
				checkStyle = ExecuteUpdateResultCheckStyle.fromResultCheckStyle( checkValue );
			}

			result.put(
					customSqlDetails.table(),
					new CustomSql( sql, isCallable, checkStyle )
			);
		} );
		return result;
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
		return getClassDetails().getDirectAnnotationUsage( DynamicInsert.class ) != null;
	}

	private boolean decodeDynamicUpdate() {
		return getClassDetails().getDirectAnnotationUsage( DynamicUpdate.class ) != null;
	}
}
