/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.domain.AllowableOutputParameterType;

/**
 * Optional {@link Type} contract for implementations that are aware of how to extract values from
 * store procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterExtractionAware<T> extends AllowableOutputParameterType<T> {
}
