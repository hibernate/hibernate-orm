/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

import org.hibernate.boot.model.naming.ImplicitAnyKeyColumnNameSource;

/**
 * @author Steve Ebersole
 */
public interface AnyKeySource extends ImplicitAnyKeyColumnNameSource {
	HibernateTypeSource getTypeSource();
	List<RelationalValueSource> getRelationalValueSources();
}
