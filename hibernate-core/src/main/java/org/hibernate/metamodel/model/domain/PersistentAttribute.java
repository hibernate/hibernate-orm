/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.metamodel.Attribute;

import org.hibernate.metamodel.model.AttributeClassification;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * Hibernate extension to the JPA {@link Attribute} contract
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<D,J,B> extends Attribute<D,J>, SqmPathSource<J,B> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	AttributeClassification getAttributeClassification();

	SimpleDomainType<?> getValueGraphType();
	SimpleDomainType<?> getKeyGraphType();
}
