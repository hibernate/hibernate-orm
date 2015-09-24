/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.converter;

import javax.persistence.AttributeConverter;

import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * For now we need to treat attributes to which a converter has been applied as mutable.
 * See Jira HHH-10111 for details.
 *
 * @author Steve Ebersole
 */
public class AttributeConverterMutabilityPlanImpl<T> extends MutableMutabilityPlan<T> {
	private final AttributeConverter attributeConverter;

	public AttributeConverterMutabilityPlanImpl(AttributeConverter attributeConverter) {
		this.attributeConverter = attributeConverter;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T deepCopyNotNull(T value) {
		return (T) attributeConverter.convertToEntityAttribute( attributeConverter.convertToDatabaseColumn( value ) );
	}
}
