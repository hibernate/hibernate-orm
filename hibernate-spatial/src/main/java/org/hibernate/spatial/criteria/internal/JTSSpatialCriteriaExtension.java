/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.spi.CriteriaBuilderExtension;
import org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder;

/**
 * @author Marco Belladelli
 */
public class JTSSpatialCriteriaExtension implements CriteriaBuilderExtension {

	@Override
	public HibernateCriteriaBuilder extend(HibernateCriteriaBuilder cb) {
		return new JTSSpatialCriteriaBuilderImpl( cb );
	}

	@Override
	public Class<? extends HibernateCriteriaBuilder> getRegistrationKey() {
		return JTSSpatialCriteriaBuilder.class;
	}
}
