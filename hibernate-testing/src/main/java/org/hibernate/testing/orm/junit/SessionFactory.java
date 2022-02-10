/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Interceptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Steve Ebersole
 */
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )

@TestInstance( TestInstance.Lifecycle.PER_CLASS )

@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ServiceRegistryExtension.class )
@ExtendWith( ServiceRegistryParameterResolver.class )

@ExtendWith( DomainModelExtension.class )
@ExtendWith( DomainModelParameterResolver.class )

@ExtendWith( SessionFactoryExtension.class )
@ExtendWith( SessionFactoryParameterResolver.class )
@ExtendWith( SessionFactoryScopeParameterResolver.class )
public @interface SessionFactory {
	String sessionFactoryName() default "";

	boolean generateStatistics() default false;
	boolean exportSchema() default true;

	boolean createSecondarySchemas() default false;

	Class<? extends Interceptor> interceptorClass() default Interceptor.class;

	Class<? extends StatementInspector> statementInspectorClass() default StatementInspector.class;

	/**
	 * Short hand for {@code statementInspectorClass = org.hibernate.testing.jdbc.SQLStatementInspector.class}
	 *
	 * @see SQLStatementInspector
	 */
	boolean useCollectingStatementInspector() default false;
}
