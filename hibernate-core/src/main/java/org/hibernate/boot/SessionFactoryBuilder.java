/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * The contract for building a {@link org.hibernate.SessionFactory} given a number of options.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface SessionFactoryBuilder {
	/**
	 * Names an interceptor to be applied to the SessionFactory, which in turn means it will be used by all
	 * Sessions unless one is explicitly specified in {@link org.hibernate.SessionBuilder#interceptor}
	 *
	 * @param interceptor The interceptor
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#INTERCEPTOR
	 */
	public SessionFactoryBuilder applyInterceptor(Interceptor interceptor);

	/**
	 * Specifies a custom entity dirtiness strategy to be applied to the SessionFactory.  See the contract
	 * of {@link org.hibernate.CustomEntityDirtinessStrategy} for details.
	 *
	 * @param strategy The custom strategy to be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CUSTOM_ENTITY_DIRTINESS_STRATEGY
	 */
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	/**
	 * Specifies a strategy for resolving the notion of a "current" tenant-identifier when using multi-tenancy
	 * together with current sessions
	 *
	 * @param resolver The resolution strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver);

	/**
	 * Specifies one or more observers to be applied to the SessionFactory.  Can be called multiple times to add
	 * additional observers.
	 *
	 * @param observers The observers to add
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers);

	/**
	 * Specifies one or more entity name resolvers to be applied to the SessionFactory (see the {@link org.hibernate.EntityNameResolver}
	 * contract for more information..  Can be called multiple times to add additional resolvers..
	 *
	 * @param entityNameResolvers The entityNameResolvers to add
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	/**
	 * Names the {@link org.hibernate.proxy.EntityNotFoundDelegate} to be applied to the SessionFactory.  EntityNotFoundDelegate is a
	 * strategy that accounts for different exceptions thrown between Hibernate and JPA when an entity cannot be found.
	 *
	 * @param entityNotFoundDelegate The delegate/strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	/**
	 * Specify the EntityTuplizerFactory to use.
	 *
	 * @param entityTuplizerFactory The EntityTuplizerFactory to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory);

	/**
	 * Register the default {@link org.hibernate.tuple.entity.EntityTuplizer} to be applied to the SessionFactory.
	 *
	 * @param entityMode The entity mode that which this tuplizer will be applied.
	 * @param tuplizerClass The custom tuplizer class.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass);

	/**
	 * Apply a Bean Validation ValidatorFactory to the SessionFactory being built.
	 *
	 * NOTE : De-typed to avoid hard dependency on Bean Validation jar at runtime.
	 *
	 * @param validatorFactory The Bean Validation ValidatorFactory to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory);

	/**
	 * Apply a CDI BeanManager to the SessionFactory being built.
	 *
	 * NOTE : De-typed to avoid hard dependency on CDI jar at runtime.
	 *
	 * @param beanManager The CDI BeanManager to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public SessionFactoryBuilder applyBeanManager(Object beanManager);

	public SessionFactoryBuilder applySqlFunction(String registrationName, SQLFunction sqlFunction);

	/**
	 * After all options have been set, build the SessionFactory.
	 *
	 * @return The built SessionFactory.
	 */
	public SessionFactory build();
}
