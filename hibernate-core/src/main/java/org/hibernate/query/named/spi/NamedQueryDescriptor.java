/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * @author Steve Ebersole
 */
public interface NamedQueryDescriptor {
	String getName();
	String getQueryString();

	Boolean getCacheable();

	String getCacheRegion();

	CacheMode getCacheMode();

	FlushMode getFlushMode();

	Boolean getReadOnly();

	LockOptions getLockOptions();

	Integer getTimeout();

	Integer getFetchSize();

	String getComment();

	QueryImplementor toQuery(SharedSessionContractImplementor session);
}
