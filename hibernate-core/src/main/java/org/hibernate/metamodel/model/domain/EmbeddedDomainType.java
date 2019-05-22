/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

/**
 * Hibernate extension to the JPA {@link } contract.
 *
 * @deprecated (since 6.0) Use {@link EmbeddableDomainType} instead.  Originally intended
 * to describe the actual usage of an embeddable (the embedded) because it was intended
 * to include the mapping (column, etc) information.  However, that causes us to need
 * multiple embeddable instances per embeddable class.
 *
 * @author Steve Ebersole
 */
@Deprecated
public interface EmbeddedDomainType<J> extends EmbeddableDomainType<J> {
}
