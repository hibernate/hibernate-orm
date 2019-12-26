/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.converter;

import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * The standard approach for defining a MutabilityPlan for converted (AttributeConverter)
 * values is to always assume that they are immutable to make sure that dirty checking,
 * deep copying and second-level caching all work properly no matter what.  That was work
 * done under https://hibernate.atlassian.net/browse/HHH-10111
 *
 * However a series of approaches to tell Hibernate that the values are immutable were
 * documented as part of https://hibernate.atlassian.net/browse/HHH-10127
 *
 * @author Steve Ebersole
 */
public class AttributeConverterMutabilityPlanImpl<T> extends MutableMutabilityPlan<T> {
	private final JpaAttributeConverter converter;

	public AttributeConverterMutabilityPlanImpl(JpaAttributeConverter converter) {
		this.converter = converter;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T deepCopyNotNull(T value) {
		return (T) converter.toDomainValue( converter.toRelationalValue( value ) );
	}
}
