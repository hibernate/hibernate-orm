/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;

import jakarta.persistence.metamodel.EntityType;

import org.hibernate.query.sqm.SqmPathSource;

/**
 * Extension to the JPA {@link EntityType} contract
 *
 * @author Steve Ebersole
 */
public interface EntityDomainType<J> extends IdentifiableDomainType<J>, EntityType<J>, SqmPathSource<J> {
	String getHibernateEntityName();

	@Override
	Collection<? extends EntityDomainType<? extends J>> getSubTypes();

	@Override
	default DomainType<J> getSqmType() {
		return this;
	}
}
