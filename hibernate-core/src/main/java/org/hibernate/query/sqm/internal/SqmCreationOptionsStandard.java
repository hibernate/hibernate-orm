/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.spi.SqmCreationOptions;

/**
 * @author Steve Ebersole
 */
public class SqmCreationOptionsStandard implements SqmCreationOptions {
	private final SessionFactoryImplementor sessionFactory;

	public SqmCreationOptionsStandard(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean useStrictJpaCompliance() {
		return sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled();
	}
}
