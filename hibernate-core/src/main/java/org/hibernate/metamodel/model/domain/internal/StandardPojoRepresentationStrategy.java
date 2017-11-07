/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StandardPojoRepresentationStrategy implements ManagedTypeRepresentationStrategy {
	private static final Logger log = Logger.getLogger( StandardPojoRepresentationStrategy.class );

	private final ReflectionOptimizer reflectionOptimizer;

	public StandardPojoRepresentationStrategy(
			ManagedTypeMapping bootMapping,
			ManagedTypeDescriptor runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.reflectionOptimizer = resolveReflectionOptimizer(
				bootMapping,
				runtimeDescriptor,
				creationContext,
				Environment.getBytecodeProvider()
		);

		log.tracef( "StandardPojoRepresentationStrategy created for [%s] with ReflectionOptimizer `%s`", runtimeDescriptor, reflectionOptimizer );
	}

	private static ReflectionOptimizer resolveReflectionOptimizer(
			ManagedTypeMapping bootModel,
			ManagedTypeDescriptor runtimeDescriptor,
			RuntimeModelCreationContext creationContext,
			BytecodeProvider bytecodeProvider) {
		if ( runtimeDescriptor.getJavaTypeDescriptor().getJavaType() == null ) {
			return null;
		}

		// todo (6.0) : extract these from the ManagedTypeDescriptor
		final String[] getterNames = null;
		final String[] setterNames = null;
		final Class[] types = null;

		try {
			return bytecodeProvider.getReflectionOptimizer(
					runtimeDescriptor.getJavaTypeDescriptor().getJavaType(),
					getterNames,
					setterNames,
					types
			);
		}
		catch (Exception e) {
			throw e;
		}
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <J> Instantiator<J> resolveInstantiator(
			ManagedTypeMapping bootModel,
			ManagedTypeDescriptor runtimeModel,
			BytecodeProvider bytecodeProvider) {
		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			return new OptimizedPojoInstantiatorImpl<>( runtimeModel.getJavaTypeDescriptor(), reflectionOptimizer );
		}
		else {
			return new PojoInstantiatorImpl<>( runtimeModel.getJavaTypeDescriptor() );
		}
	}

	@Override
	public PropertyAccess generatePropertyAccess(
			ManagedTypeMapping bootDescriptor,
			PersistentAttributeMapping bootAttribute,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider) {
		PropertyAccessStrategy strategy = null;

		final BuiltInPropertyAccessStrategies namedStrategy = BuiltInPropertyAccessStrategies.interpret( bootAttribute.getPropertyAccessorName() );
		if ( namedStrategy != null ) {
			strategy = namedStrategy.getStrategy();
		}

		if ( strategy == null ) {
			// for now...
			strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
		}

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							bootDescriptor.getName(),
							bootAttribute.getName()
					)
			);
		}

		return strategy.buildPropertyAccess(
				runtimeDescriptor.getJavaTypeDescriptor().getJavaType(),
				bootAttribute.getName()
		);
	}
}
