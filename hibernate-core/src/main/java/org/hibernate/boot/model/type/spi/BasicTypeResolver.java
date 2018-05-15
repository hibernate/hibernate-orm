/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.type.spi.BasicType;

/**
 * Describes the data needed to resolve a BasicType.
 * <p/>
 * Generally speaking, this gives the resolver access directly back to the persistent
 * attribute's metadata.
 * <p/>
 * This interface caters to 2 forms of BasicType definition for an attribute:<ol>
 *     <li>
 *         The first is by explicitly defining a type to use via {@link #getExplicitTypeName()},
 *         e.g. through {@link org.hibernate.annotations.Type}.  This name can be resolved in
 *         a number of ways:
 *         <ul>
 *             <li>
 *                 If the name refers to an existing entry in the
 *             </li>
 *         </ul>N
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface BasicTypeResolver {
	<T> BasicType<T> resolveBasicType(ResolutionContext context);
}
