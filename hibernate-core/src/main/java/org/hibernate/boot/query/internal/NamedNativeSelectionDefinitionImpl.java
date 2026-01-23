/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.SqlResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;

import java.util.Map;
import java.util.Set;

import static org.hibernate.boot.query.internal.Helper.extractHints;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.setOf;

/// Boot-time model of a named native selection query.
///
/// @see NamedNativeQuery
/// @see jakarta.persistence.NamedNativeQuery
///
/// @author Steve Ebersole
public class NamedNativeSelectionDefinitionImpl<R> extends AbstractNamedSelectionDefinition<R> implements NamedNativeQueryDefinition<R> {
	private final String sqlString;
	@Nullable
	private final Class<R> resultType;
	private final String resultSetMappingName;
	private final Set<String> querySpaces;

	public NamedNativeSelectionDefinitionImpl(
			String name,
			@Nullable String location,
			String sqlString,
			@Nullable Class<R> resultType,
			String resultSetMappingName,
			FlushMode flushMode,
			Timeout timeout,
			String comment,
			Boolean readOnly,
			Integer fetchSize,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			CacheMode cacheMode,
			String cacheRegion,
			Set<String> querySpaces,
			Map<String,Object> hints) {
		super(
				name,
				location,
				flushMode,
				timeout,
				comment,
				readOnly,
				fetchSize,
				firstResult,
				maxResults,
				cacheable,
				cacheRegion,
				cacheMode,
				null,
				null,
				null,
				null,
				hints
		);
		this.resultType = resultType;
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.querySpaces = querySpaces;
	}

	@Override
	public String getSqlQueryString() {
		return sqlString;
	}

	@Override
	public String getQueryString() {
		return getSqlQueryString();
	}

	@Override
	public @Nullable Class<R> getResultType() {
		return resultType;
	}

	@Override
	public String getEntityGraphName() {
		return null;
	}

	@Override
	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NamedNativeQueryMemento<R> resolve(SessionFactoryImplementor factory) {
		return new NativeSelectionMementoImpl<>(
				name,
				sqlString,
				resultType,
				resultSetMappingName,
				querySpaces,
				flushMode,
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
	public static NamedNativeSelectionDefinitionImpl<?> from(NamedNativeQuery annotation, AnnotationTarget location) {
		return new NamedNativeSelectionDefinitionImpl<>(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				StringHelper.nullIfEmpty( annotation.resultSetMapping() ),
				interpret( annotation.flush(), annotation.flushMode() ),
				cleanTimeout( annotation.timeout() ),
				StringHelper.nullIfEmpty( annotation.comment() ),
				annotation.readOnly(),
				annotation.fetchSize(),
				null,
				null,
				annotation.cacheable(),
				CacheMode.resolve( annotation.cacheMode(), annotation.cacheRetrieveMode(), annotation.cacheStoreMode() ),
				StringHelper.nullIfEmpty( annotation.cacheRegion() ),
				Set.of( annotation.querySpaces() ),
				Map.of()
		);
	}

	private static FlushMode interpret(QueryFlushMode queryFlushMode, FlushModeType flushModeType) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			if ( flushModeType == FlushModeType.PERSISTENCE_CONTEXT ) {
				return null;
			}
			return flushModeType.toFlushMode();
		}
		return queryFlushMode == QueryFlushMode.FLUSH
				? FlushMode.ALWAYS
				: FlushMode.MANUAL;
	}

	/// Build a definition from JPA's [jakarta.persistence.NamedNativeQuery] annotation.
	///
	/// @param annotation The annotation.
	/// @param location Where the annotation was found.
	public static NamedNativeSelectionDefinitionImpl<?> from(jakarta.persistence.NamedNativeQuery annotation, AnnotationTarget location) {
		return new NamedNativeSelectionDefinitionImpl<>(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				StringHelper.nullIfEmpty( annotation.resultSetMapping() ),
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
				setOf( sqlSelect.querySpaces() ),
				null
		);
	}
}
