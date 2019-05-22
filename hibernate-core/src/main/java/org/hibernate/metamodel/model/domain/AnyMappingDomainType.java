/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

/**
 * Models Hibernate's ANY mapping (reverse discrimination) as a JPA domain model type
 *
 * @param <J> The base Java type defined for the any mapping
 *
 * @author Steve Ebersole
 */
public interface AnyMappingDomainType<J> extends SimpleDomainType<J> {
}
