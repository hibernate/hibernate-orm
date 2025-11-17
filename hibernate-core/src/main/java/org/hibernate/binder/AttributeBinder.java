/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import org.hibernate.Incubating;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.lang.annotation.Annotation;

/**
 * Allows a user-written annotation to drive some customized model binding.
 * <p>
 * An implementation of this interface interacts directly with model objects
 * like {@link PersistentClass} and {@link Property} to implement the
 * semantics of some {@linkplain org.hibernate.annotations.AttributeBinderType
 * custom mapping annotation}.
 *
 * @see org.hibernate.annotations.AttributeBinderType
 * @see TypeBinder
 *
 * @author Gavin King
 */
@Incubating
public interface AttributeBinder<A extends Annotation> {
	/**
	 * Perform some custom configuration of the model relating to the given annotated
	 * {@link Property} of the given {@linkplain PersistentClass entity class} or
	 * {@linkplain org.hibernate.mapping.Component embeddable class}.
	 *
	 * @param annotation an annotation of the property that is declared as an
	 *                   {@link org.hibernate.annotations.AttributeBinderType}
	 * @param persistentClass the entity class acting as the ultimate container of the
	 *                        property (differs from {@link Property#getPersistentClass()}
	 *                        in the case of a property of an embeddable class)
	 * @param property a {@link Property} object representing the annotated property
	 */
	void bind(A annotation, MetadataBuildingContext buildingContext, PersistentClass persistentClass, Property property);
}
