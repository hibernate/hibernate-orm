/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.service.Service;

/**
 * Interface which allows extension of {@link HibernateCriteriaBuilder}
 * with additional functionality by registering a {@link Service}.
 *
 * @author Marco Belladelli
 */
public interface CriteriaBuilderExtension extends Service {

	HibernateCriteriaBuilder extend(HibernateCriteriaBuilder cb);

	Class<? extends HibernateCriteriaBuilder> getRegistrationKey();
}
