/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

/**
 * @author Jan Schatteman
 */
public interface JaxbNamedQuery extends JaxbQueryHintContainer {
	String getQuery();
	String getComment();
	Timeout getTimeout();
	Boolean isCacheable();
	String getCacheRegion();
	Integer getFetchSize();
	Boolean isReadOnly();
	List<JaxbQueryParamTypeImpl> getQueryParam();
	CacheMode getCacheMode();
	FlushMode getFlushMode();
}
