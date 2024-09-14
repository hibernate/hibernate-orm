/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

/**
 * @author Jan Schatteman
 */
public interface JaxbNamedQuery extends JaxbQueryHintContainer {
	String getQuery();
	String getComment();
	Integer getTimeout();
	Boolean isCacheable();
	String getCacheRegion();
	Integer getFetchSize();
	Boolean isReadOnly();
	List<JaxbQueryParamTypeImpl> getQueryParam();
	CacheMode getCacheMode();
	FlushMode getFlushMode();
}
