/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Interceptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.junit5.FailureExpectedExtension;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target(ElementType.TYPE)
@Inherited
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( DialectFilterExtension.class )
@ExtendWith( ServiceRegistryExtension.class )
@ExtendWith( TestDomainExtension.class )
@ExtendWith( SessionFactoryExtension.class )
@ExtendWith( SessionFactoryParameterResolver.class )
public @interface SessionFactory {
	String sessionFactoryName() default "";

	boolean generateStatistics() default false;

	Class<? extends Interceptor> interceptorClass() default Interceptor.class;

	Class<? extends StatementInspector> statementInspectorClass() default StatementInspector.class;
}
