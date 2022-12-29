/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
