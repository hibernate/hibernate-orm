/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Method;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.ServiceRegistry;

/**
 * Standard implementation of PropertyAccessStrategyResolver
 *
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyResolverStandardImpl implements PropertyAccessStrategyResolver {
	private final ServiceRegistry serviceRegistry;

	public PropertyAccessStrategyResolverStandardImpl(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public PropertyAccessStrategy resolvePropertyAccessStrategy(
			Class containerClass,
			String explicitAccessStrategyName,
			EntityMode entityMode) {

		if ( hasBytecodeEnhancedAttributes( containerClass ) ) {
			return PropertyAccessStrategyEnhancedImpl.INSTANCE;
		}

		if ( StringHelper.isNotEmpty( explicitAccessStrategyName ) ) {
			return resolveExplicitlyNamedPropertyAccessStrategy( explicitAccessStrategyName );
		}

		if ( entityMode == EntityMode.MAP ) {
			return BuiltInPropertyAccessStrategies.MAP.getStrategy();
		}
		else {
			return BuiltInPropertyAccessStrategies.BASIC.getStrategy();
		}
	}

	protected PropertyAccessStrategy resolveExplicitlyNamedPropertyAccessStrategy(String explicitAccessStrategyName) {
		final BuiltInPropertyAccessStrategies builtInStrategyEnum = BuiltInPropertyAccessStrategies.interpret(
				explicitAccessStrategyName
		);
		if ( builtInStrategyEnum != null ) {
			return builtInStrategyEnum.getStrategy();
		}

		return strategySelectorService().resolveStrategy( PropertyAccessStrategy.class, explicitAccessStrategyName );
	}

	private StrategySelector strategySelectorService;

	protected StrategySelector strategySelectorService() {
		if ( strategySelectorService == null ) {
			if ( serviceRegistry == null ) {
				throw new HibernateException( "ServiceRegistry not yet injected; PropertyAccessStrategyResolver not ready for use." );
			}
			strategySelectorService = serviceRegistry.getService( StrategySelector.class );
		}
		return strategySelectorService;
	}

	private boolean hasBytecodeEnhancedAttributes(Class<?> containerClass) {
		for ( Method m : containerClass.getDeclaredMethods() ) {
			if ( m.getName().startsWith( EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX ) ||
					m.getName().startsWith( EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX ) ) {
				return true;
			}
		}
		return false;
	}

}
