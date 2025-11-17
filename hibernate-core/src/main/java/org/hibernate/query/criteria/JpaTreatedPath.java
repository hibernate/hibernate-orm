/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaTreatedPath<T,S extends T> extends JpaPath<S> {
	ManagedDomainType<S> getTreatTarget();
}
