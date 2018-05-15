/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.metamodel.Attribute;

/**
 * Hibernate extension to the JPA {@link Attribute} contract
 *
 * @apiNote Hibernate's attributes differ from JPA's conceptually in
 * that Hibernate's inherently encompass the mapping metadata for the
 * attribute whereas JPA inherently excludes it.  This is much more
 * useful information, but leads to some inconsistencies in the model
 * namely around composites/embeddeds - see {@link EmbeddedDomainType}.
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<D,J> extends Attribute<D,J> {
	/**
	 * The attribute's type
	 */
	DomainType<J> getAttributeType();

	@Override
	ManagedDomainType<D> getDeclaringType();

	SimpleDomainType<?> getValueGraphType();
	SimpleDomainType<?> getKeyGraphType();
}
