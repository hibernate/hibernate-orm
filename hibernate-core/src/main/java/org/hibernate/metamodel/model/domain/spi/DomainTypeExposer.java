/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.DomainType;

/**
 * A unification of anything in the runtime domain metamodel that exposes
 * the domain type it represents
 *
 * @author Steve Ebersole
 */
public interface DomainTypeExposer<J> {
	DomainType<J> getDomainType();
}
