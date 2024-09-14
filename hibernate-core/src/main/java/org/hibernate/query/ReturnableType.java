/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.SimpleDomainType;

/**
 * Specialization of DomainType for types that can be used as function returns
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ReturnableType<T> extends SimpleDomainType<T> {
}
