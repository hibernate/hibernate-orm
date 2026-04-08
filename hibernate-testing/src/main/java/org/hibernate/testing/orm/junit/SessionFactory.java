/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import org.hibernate.Interceptor;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/// Defines the SessionFactory to be used for testing.
/// Produces a [SessionFactoryScope] which can be injected via [JUnit][SessionFactoryScopeParameterResolver]
/// or via [SessionFactoryScopeAware]; the JUnit approach should be preferred.
///
/// ```java
/// @DomainModel(annotatedClasses=SomeEntity.class)
/// @SessionFactory
/// class SomeTest {
///     @Test
///     void testStuff(SessionFactoryScope factoryScope) {
///         ...
///     }
/// }
/// ```
///
/// @see SessionFactoryExtension
/// @see SessionFactoryScope
///
/// @author Steve Ebersole
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
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

	/**
	 * @deprecated Use {@linkplain #useCollectingStatementObserver()} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	Class<? extends StatementInspector> statementInspectorClass() default StatementInspector.class;

	/**
	 * @deprecated Use {@linkplain #useCollectingStatementObserver()} instead.
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	boolean useCollectingStatementInspector() default false;

	/**
	 * Triggers the use of {@linkplain CollectingStatementObserver} as the SessionFactory's
	 * {@linkplain org.hibernate.cfg.JdbcSettings#STATEMENT_OBSERVER statement observer}.
	 * Can be accessed using {@linkplain SessionFactoryScope#getCollectingStatementObserver()}
	 */
	boolean useCollectingStatementObserver() default false;

	boolean applyCollectionsInDefaultFetchGroup() default true;

	Class<? extends Consumer<SessionFactoryBuilder>> sessionFactoryConfigurer() default NoOpConfigurer.class;

	class NoOpConfigurer implements Consumer<SessionFactoryBuilder> {

		@Override
		public void accept(SessionFactoryBuilder sessionFactoryBuilder) {
		}
	}
}
