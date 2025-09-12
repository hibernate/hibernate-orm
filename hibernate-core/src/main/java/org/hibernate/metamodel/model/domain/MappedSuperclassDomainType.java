/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.sqm.SqmPathSource;

import jakarta.persistence.metamodel.MappedSuperclassType;

/**
 * Extension of the JPA {@link MappedSuperclassType} contract
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassDomainType<J> extends IdentifiableDomainType<J>, MappedSuperclassType<J>, SqmPathSource<J> {
	@Override
	default DomainType<J> getSqmType() {
		return IdentifiableDomainType.super.getSqmType();
	}
}
