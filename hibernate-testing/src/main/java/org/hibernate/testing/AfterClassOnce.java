/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a method which should be run once after the last
 * test execution for the given class.  Much like JUnit's own {@link org.junit.AfterClass},
 * except this annotation need not be attached to a static method
 *
 * @author Steve Ebersole
 * @deprecated Use JUnit 5 along with one of the Hibernate ORM's class-level test annotations
 * ({@link org.hibernate.testing.orm.junit.BaseUnitTest},
 * {@link org.hibernate.testing.orm.junit.SessionFactory},
 * {@link org.hibernate.testing.orm.junit.Jpa},
 * {@link org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting},
 * {@link org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting}, ...)
 * and {@link org.junit.jupiter.api.AfterAll}.
 * Alternatively to the Hibernate ORM test annotations,
 * you can use {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)} directly on your test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Deprecated(forRemoval = true)
public @interface AfterClassOnce {
}
