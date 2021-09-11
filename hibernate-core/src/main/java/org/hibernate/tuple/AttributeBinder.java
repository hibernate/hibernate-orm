/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.Incubating;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.lang.annotation.Annotation;

/**
 * Allows a user-written annotation to drive some customized model binding.
 *
 * @see org.hibernate.annotations.AttributeBinderType
 *
 * @author Gavin King
 */
@Incubating
public interface AttributeBinder<A extends Annotation> {
	/**
	 * Perform some custom configuration of the model relating to the given {@link Property}
	 * of the given {@link PersistentClass}.
	 */
	void bind(A annotation, MetadataBuildingContext buildingContext, PersistentClass persistentClass, Property property);
}
