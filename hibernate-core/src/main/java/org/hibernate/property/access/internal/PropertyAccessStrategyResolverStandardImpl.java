/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.engine.internal.ManagedTypeHelper.isManagedType;

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
			Class<?> containerClass,
			String explicitAccessStrategyName,
			RepresentationMode representationMode) {

		if ( BuiltInPropertyAccessStrategies.BASIC.getExternalName().equals( explicitAccessStrategyName )
				|| BuiltInPropertyAccessStrategies.FIELD.getExternalName().equals( explicitAccessStrategyName )
				|| BuiltInPropertyAccessStrategies.MIXED.getExternalName().equals( explicitAccessStrategyName ) ) {
			//type-cache-pollution agent: it's crucial to use the ManagedTypeHelper rather than attempting a direct cast
			if ( isManagedType( containerClass ) ) {
				if ( AccessType.FIELD.getType().equals( explicitAccessStrategyName ) ) {
					return PropertyAccessStrategyEnhancedImpl.FIELD;
				}
				else if ( AccessType.PROPERTY.getType().equals( explicitAccessStrategyName ) ) {
					return PropertyAccessStrategyEnhancedImpl.PROPERTY;
				}
				return PropertyAccessStrategyEnhancedImpl.STANDARD;
			}
		}

		if ( StringHelper.isNotEmpty( explicitAccessStrategyName ) ) {
			return resolveExplicitlyNamedPropertyAccessStrategy( explicitAccessStrategyName );
		}

		if ( representationMode == RepresentationMode.MAP ) {
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
			strategySelectorService = serviceRegistry.requireService( StrategySelector.class );
		}
		return strategySelectorService;
	}

}
