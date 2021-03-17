/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.SimpleDomainType;

/**
 * @author Steve Ebersole
 */
public interface SimpleTypeDescriptor<J> extends SimpleDomainType<J>, DomainTypeDescriptor<J> {
}
