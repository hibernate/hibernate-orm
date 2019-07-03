/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.TemporalType;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * Specialization of DomainType for types that can be used as temporal bind values
 * for a query parameter
 *
 * @author Steve Ebersole
 */
public interface AllowableTemporalParameterType<T> extends AllowableParameterType<T> {
	/**
	 * Convert the value and/or type to the specified temporal precision
	 */
	AllowableTemporalParameterType resolveTemporalPrecision(TemporalType temporalPrecision, TypeConfiguration typeConfiguration);
}
