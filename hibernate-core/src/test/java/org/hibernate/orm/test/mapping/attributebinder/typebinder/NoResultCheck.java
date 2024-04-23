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
			try {
				persistentClass.setInsertExpectation(Expectation.None.class.getDeclaredConstructor());
				persistentClass.setUpdateExpectation(Expectation.None.class.getDeclaredConstructor());
				persistentClass.setDeleteExpectation(Expectation.None.class.getDeclaredConstructor());
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void bind(NoResultCheck annotation, MetadataBuildingContext buildingContext, Component embeddableClass) {

		}
	}
}
