/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder.typebinder;

import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.EntityBindingContext;
import org.hibernate.binder.TypeBinder;
import org.hibernate.jdbc.Expectation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@TypeBinderType(binder = NoResultCheck.Binder.class)
@Retention(RUNTIME)
@Target(TYPE)
public @interface NoResultCheck {
	class Binder implements TypeBinder<NoResultCheck> {
		@Override
		public void bind(NoResultCheck annotation, EntityBindingContext context) {
			final var persistentClass = context.getPersistentClass();
			persistentClass.setInsertExpectation(Expectation.None::new);
			persistentClass.setUpdateExpectation(Expectation.None::new);
			persistentClass.setDeleteExpectation(Expectation.None::new);
		}
	}
}
