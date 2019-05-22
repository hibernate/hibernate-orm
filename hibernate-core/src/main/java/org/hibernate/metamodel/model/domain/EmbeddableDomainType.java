/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.metamodel.EmbeddableType;

/**
 * Hibernate extension to the JPA {@link EmbeddableType} contract.
 *
 * @apiNote Temporarily extends the deprecated EmbeddableType.  See the {@link EmbeddableType}
 * Javadocs for more information
 *
 * @author Steve Ebersole
 */
public interface EmbeddableDomainType<J> extends ManagedDomainType<J>, EmbeddableType<J> {
}
