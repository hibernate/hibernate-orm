/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Hibernate extension to the JPA {@link Attribute} contract
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<D,J> extends Attribute<D,J> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	JavaType<J> getAttributeJavaType();

	/**
	 * The classification of the attribute (is it a basic type, entity, etc)
	 */
	AttributeClassification getAttributeClassification();

	DomainType<?> getValueGraphType();
	SimpleDomainType<?> getKeyGraphType();
}
