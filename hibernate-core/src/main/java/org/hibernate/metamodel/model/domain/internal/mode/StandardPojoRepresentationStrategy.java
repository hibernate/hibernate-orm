/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StandardPojoRepresentationStrategy implements ManagedTypeRepresentationStrategy {
	private static final Logger log = Logger.getLogger( StandardPojoRepresentationStrategy.class );

	private final StrategySelector strategySelector;
	private Supplier<ReflectionOptimizer> reflectionOptimizerAccess;

	public StandardPojoRepresentationStrategy(
			ManagedTypeMapping bootDescriptor,
			ManagedTypeDescriptor runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
//		if ( runtimeDescriptor.getJavaTypeDescriptor().getJavaType() == null ) {
//			// todo (6.0) : is this legal?  pojo with no java-type?!?
//			this.reflectionOptimizerAccess = null;
//		}
//		else {
//			this.reflectionOptimizerAccess = () -> buildReflectionOptimizer( bootDescriptor, runtimeDescriptor, creationContext, Environment.getBytecodeProvider() );
//		}

		this.strategySelector = creationContext.getSessionFactory()
				.getServiceRegistry()
				.getService( StrategySelector.class );

		log.tracef( "StandardPojoRepresentationStrategy created for [%s]", runtimeDescriptor );
	}

	private static ReflectionOptimizer buildReflectionOptimizer(
			ManagedTypeMapping bootDescriptor,
			ManagedTypeDescriptor runtimeDescriptor,
			RuntimeModelCreationContext creationContext,
			BytecodeProvider bytecodeProvider) {
		if ( runtimeDescriptor.getJavaTypeDescriptor().getJavaType() == null ) {
			// todo (6.0) : is this legal?  pojo with no java-type?!?
			return null;
		}

		final List<String> getterNames = new ArrayList<>();
		final List<String> setterNames = new ArrayList<>();
		final List<JavaTypeDescriptor> types = new ArrayList<>();

		//noinspection Convert2Lambda
		runtimeDescriptor.visitAttributes(
				new Consumer<NonIdPersistentAttribute>() {
					@Override
					public void accept(NonIdPersistentAttribute attribute) {
						getterNames.add( attribute.getPropertyAccess().getGetter().getMethodName() );
						setterNames.add( attribute.getPropertyAccess().getSetter().getMethodName() );
						types.add( attribute.getJavaTypeDescriptor() );
					}
				}
		);

		try {
			return bytecodeProvider.getReflectionOptimizer(
					runtimeDescriptor.getJavaTypeDescriptor().getJavaType(),
					getterNames.toArray( new String[0] ),
					setterNames.toArray( new String[0] ),
					types.toArray( new JavaTypeDescriptor[0] )
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
			ManagedTypeMapping bootDescriptor,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider) {
		final ReflectionOptimizer reflectionOptimizer = reflectionOptimizerAccess == null ? null : reflectionOptimizerAccess.get();

		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			return new OptimizedPojoInstantiatorImpl<>(
					runtimeDescriptor.getJavaTypeDescriptor(),
					reflectionOptimizer
			);
		}
		else {
			return new PojoInstantiatorImpl<>( runtimeDescriptor.getJavaTypeDescriptor() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <J> ProxyFactory generateProxyFactory(
			AbstractEntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		return StandardPojoProxyFactoryInstantiator.INSTANCE.instantiate( runtimeDescriptor, creationContext );
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
			if ( StringHelper.isNotEmpty( bootAttribute.getPropertyAccessorName() ) ) {
				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy(
						PropertyAccessStrategy.class,
						bootAttribute.getPropertyAccessorName()
				);
			}
			else {
				// for now...
				strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
			}
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
