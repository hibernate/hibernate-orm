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
package org.hibernate.internal;

import org.hibernate.BasicQueryContract;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicQueryContractImpl implements BasicQueryContract {
	private final SessionImplementor session;

	private FlushMode flushMode;
	private CacheMode cacheMode;

	private boolean cacheable;
	private String cacheRegion;
	private boolean readOnly;
	private RowSelection selection = new RowSelection();

	protected AbstractBasicQueryContractImpl(SessionImplementor session) {
		this.session = session;
		this.readOnly = session.getPersistenceContext().isDefaultReadOnly();
	}

	protected SessionImplementor session() {
		return session;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public BasicQueryContract setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public BasicQueryContract setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public BasicQueryContract setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public BasicQueryContract setCacheRegion(String cacheRegion) {
		if ( cacheRegion != null ) {
			this.cacheRegion = cacheRegion.trim();
		}
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public BasicQueryContract setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public Integer getTimeout() {
		return selection.getTimeout();
	}

	@Override
	public BasicQueryContract setTimeout(int timeout) {
		selection.setTimeout( timeout );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return selection.getFetchSize();
	}

	@Override
	public BasicQueryContract setFetchSize(int fetchSize) {
		selection.setFetchSize( fetchSize );
		return this;
	}

	public QueryParameters buildQueryParametersObject() {
		QueryParameters qp = new QueryParameters();
		qp.setRowSelection( selection );
		qp.setCacheable( cacheable );
		qp.setCacheRegion( cacheRegion );
		qp.setReadOnly( readOnly );
		return qp;
	}
}
