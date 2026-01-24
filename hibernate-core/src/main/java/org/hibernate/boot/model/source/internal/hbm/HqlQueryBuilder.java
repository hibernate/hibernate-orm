/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.query.internal.NamedHqlSelectionDefinitionImpl;
import org.hibernate.models.spi.AnnotationTarget;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.interpretFlushMode;

/// Used to build NamedHqlSelectionDefinitionImpl references
/// from named queries defined in hbm.xml mappings.
///
/// @author Steve Ebersole
public class HqlQueryBuilder<E> extends AbstractNamedQueryBuilder<E, HqlQueryBuilder<E>> {
	private String hqlString;

	private String entityGraphName;

	private Integer firstResult;
	private Integer maxResults;

	private Map<String, String> parameterTypes;

	public HqlQueryBuilder(String name, AnnotationTarget location) {
		super( name, location );
	}

	public HqlQueryBuilder(String name) {
		super( name, null );
	}

	@Override
	protected HqlQueryBuilder<E> getThis() {
		return this;
	}

	public String getHqlString() {
		return hqlString;
	}

	public HqlQueryBuilder<E> setHqlString(String hqlString) {
		this.hqlString = hqlString;
		return this;
	}

	public String getEntityGraphName() {
		return entityGraphName;
	}

	public HqlQueryBuilder<E> setEntityGraphName(String entityGraphName) {
		this.entityGraphName = entityGraphName;
		return this;
	}

	public HqlQueryBuilder<E> setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
		return getThis();
	}

	public HqlQueryBuilder<E> setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return getThis();
	}

	public NamedHqlSelectionDefinitionImpl<E> build() {
		return new NamedHqlSelectionDefinitionImpl<>(
				getName(),
				getLocation() == null ? null : getLocation().getName(),
				hqlString,
				getResultClass(),
				entityGraphName,
				interpretFlushMode( flushMode ),
				timeout,
				comment,
				readOnly,
				fetchSize,
				firstResult,
				maxResults,
				cacheable,
				cacheMode,
				cacheRegion,
				null,
				null,
				null,
				null,
				parameterTypes,
				getHints()
		);
	}

	public void addParameterTypeHint(String name, String type) {
		if ( parameterTypes == null ) {
			parameterTypes = new HashMap<>();
		}

		parameterTypes.put( name, type );
	}
}
