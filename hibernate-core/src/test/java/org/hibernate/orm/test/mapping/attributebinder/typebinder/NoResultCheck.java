/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.attributebinder.typebinder;

import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

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
		public void bind(NoResultCheck annotation, MetadataBuildingContext buildingContext, PersistentClass persistentClass) {
			persistentClass.setInsertExpectation(Expectation.None::new);
			persistentClass.setUpdateExpectation(Expectation.None::new);
			persistentClass.setDeleteExpectation(Expectation.None::new);
		}

		@Override
		public void bind(NoResultCheck annotation, MetadataBuildingContext buildingContext, Component embeddableClass) {

		}
	}
}
