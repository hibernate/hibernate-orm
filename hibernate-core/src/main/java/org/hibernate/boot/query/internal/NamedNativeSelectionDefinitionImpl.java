/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Timeout;
import jakarta.persistence.PessimisticLockScope;
import jakarta.annotation.Nullable;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.query.internal.Helper.extractHints;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOf;

/// Boot-time model of a named native selection query.
///
/// @see NamedNativeQuery
/// @see jakarta.persistence.NamedNativeQuery
///
/// @author Steve Ebersole
public class NamedNativeSelectionDefinitionImpl<R> extends AbstractNamedSelectionDefinition<R> implements NamedNativeQueryDefinition<R> {
	@Nonnull
	private final String sqlString;
	@Nullable
	private final Class<R> resultType;
	@Nullable
	private final String resultSetMappingName;
	@Nullable
	private final Set<String> querySpaces;

	public NamedNativeSelectionDefinitionImpl(
			@Nonnull String name,
			@Nullable String location,
			@Nonnull String sqlString,
			@Nullable Class<R> resultType,
			@Nullable String resultSetMappingName,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nullable Boolean readOnly,
			@Nullable Integer fetchSize,
			@Nullable Integer firstResult,
			@Nullable Integer maxResults,
			@Nullable Boolean cacheable,
			@Nullable CacheMode cacheMode,
			@Nullable String cacheRegion,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nullable Set<String> querySpaces,
			@Nonnull Map<String,Object> hints) {
		super(
				name,
				location,
				queryFlushMode,
				timeout,
				comment,
				readOnly,
				fetchSize,
				firstResult,
				maxResults,
				cacheable,
				cacheRegion,
				cacheMode,
				lockMode,
				lockScope,
				lockTimeout,
				followOnLockingStrategy,
				hints
		);
		this.resultType = resultType;
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.querySpaces = querySpaces;
	}

	@Nonnull
	@Override
	public String getSqlQueryString() {
		return sqlString;
	}

	@Nonnull
	@Override
	public String getQueryString() {
		return getSqlQueryString();
	}

	@Override
	@Nullable
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}

	@Nullable
	@Override
	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	@Nullable
	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Nonnull
	@Override
	public NamedNativeQueryMemento<R> resolve(@Nonnull SessionFactoryImplementor factory) {
		return new NativeSelectionMementoImpl<>(
				name,
				sqlString,
				resultType,
				resultSetMappingName,
				querySpaces,
				queryFlushMode,
				timeout,
				comment,
				readOnly,
				fetchSize,
				firstRow,
				maxRows,
				cacheable,
				cacheMode,
				cacheRegion,
				lockMode,
				lockScope,
				lockTimeout,
				followOnLockingStrategy,
				getHints()
		);
	}

	/// Build a definition from Hibernate's [NamedNativeQuery] annotation.
	///
	/// @param annotation The annotation.
	/// @param location Where the annotation was found.
	@Nonnull
	public static NamedNativeSelectionDefinitionImpl<?> from(
			@Nonnull NamedNativeQuery annotation,
			@Nullable AnnotationTarget location) {
		return new NamedNativeSelectionDefinitionImpl<>(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				nullIfEmpty( annotation.resultSetMapping() ),
				annotation.flush(),
				cleanTimeout( annotation.timeout() ),
				nullIfEmpty( annotation.comment() ),
				annotation.readOnly(),
				annotation.fetchSize(),
				null,
				null,
				annotation.cacheable(),
				CacheMode.resolve( annotation.cacheMode(), annotation.cacheRetrieveMode(), annotation.cacheStoreMode() ),
				nullIfEmpty( annotation.cacheRegion() ),
				null,
				null,
				null,
				null,
				Set.of( annotation.querySpaces() ),
				Map.of()
		);
	}

	/// Build a definition from JPA's [jakarta.persistence.NamedNativeQuery] annotation.
	///
	/// @param annotation The annotation.
	/// @param location Where the annotation was found.
	@Nonnull
	public static NamedNativeSelectionDefinitionImpl<?> from(
			@Nonnull jakarta.persistence.NamedNativeQuery annotation,
			@Nullable AnnotationTarget location) {
		return from( annotation, location, annotation.resultSetMapping() );
	}

	@Nonnull
	public static NamedNativeSelectionDefinitionImpl<?> from(
			@Nonnull jakarta.persistence.NamedNativeQuery annotation,
			@Nullable AnnotationTarget location,
			@Nullable String resultSetMappingName) {
		return new NamedNativeSelectionDefinitionImpl<>(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				nullIfEmpty( resultSetMappingName ),
				annotation.flush(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				extractHints( annotation.hints() )
		);
	}

	public static NamedNativeSelectionDefinitionImpl<?> from(
			String name,
			SQLSelect sqlSelect,
			ClassDetails location, MetadataBuildingContext context) {
		final String locationName;
		final Class<?> resultType;
		if ( location == null ) {
			locationName = null;
			resultType = null;
		}
		else {
			locationName = location.getClassName();
			resultType = location.toJavaClass();
		}


		String resultSetMappingName = null;
		final var resultSetMapping = sqlSelect.resultSetMapping();
		if ( !isEmpty( resultSetMapping.columns() )
			|| !isEmpty( resultSetMapping.entities() )
			|| !isEmpty( resultSetMapping.classes() ) ) {
			context.getMetadataCollector()
					.addResultSetMapping( SqlResultSetMappingDescriptor.from( resultSetMapping, name ) );
			resultSetMappingName = name;
		}

		return new NamedNativeSelectionDefinitionImpl<>(
				name,
				locationName,
				sqlSelect.sql(),
				resultType,
				resultSetMappingName,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				setOf( sqlSelect.querySpaces() ),
				emptyMap()
		);
	}
}
