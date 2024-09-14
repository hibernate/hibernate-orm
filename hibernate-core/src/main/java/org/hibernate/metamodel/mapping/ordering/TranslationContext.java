/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;

/**
 * Access to information needed while translating a collection's order-by fragment
 *
 * @author Steve Ebersole
 */
public interface TranslationContext {

	SessionFactoryImplementor getFactory();

	default JpaCompliance getJpaCompliance() {
		return getFactory().getSessionFactoryOptions().getJpaCompliance();
	}
}
