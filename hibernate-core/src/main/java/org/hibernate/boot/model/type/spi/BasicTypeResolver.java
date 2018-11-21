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
 * Support for resolving an explicitly named BasicType.  Can be named either by: <ul>
 *     <li>{@link org.hibernate.annotations.TypeDef}</li>
 *     <li>{@link org.hibernate.annotations.Type}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface BasicTypeResolver {
	<T> BasicType<T> resolveBasicType(ResolutionContext context);
}
