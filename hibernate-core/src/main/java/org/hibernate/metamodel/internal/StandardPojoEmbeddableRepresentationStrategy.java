/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.Instantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * @author Steve Ebersole
 */
public class StandardPojoEmbeddableRepresentationStrategy extends AbstractEmbeddableRepresentationStrategy {
	private final StrategySelector strategySelector;

	private final ReflectionOptimizer reflectionOptimizer;
	private final Instantiator instantiator;

	public StandardPojoEmbeddableRepresentationStrategy(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		//noinspection unchecked
		super(
				bootDescriptor,
				creationContext.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.resolveDescriptor( bootDescriptor.getComponentClass() ),
				creationContext
		);


		assert bootDescriptor.getComponentClass() != null;

		this.strategySelector = creationContext.getSessionFactory()
				.getServiceRegistry()
				.getService( StrategySelector.class );

		this.reflectionOptimizer = buildReflectionOptimizer( bootDescriptor, creationContext );
		final ConfigurationService configurationService = creationContext.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
				.getService(ConfigurationService.class);
		boolean createEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				Environment.CREATE_EMPTY_COMPOSITES_ENABLED,
				configurationService.getSettings(),
				false
		);

		if ( reflectionOptimizer != null && reflectionOptimizer.getInstantiationOptimizer() != null ) {
			final ReflectionOptimizer.InstantiationOptimizer instantiationOptimizer = reflectionOptimizer.getInstantiationOptimizer();
			this.instantiator = instantiationOptimizer != null
					? new OptimizedPojoInstantiatorImpl<>( getEmbeddableJavaTypeDescriptor(), instantiationOptimizer )
					: new PojoInstantiatorImpl<>( getEmbeddableJavaTypeDescriptor() );
		}
		else {
			this.instantiator = new PojoInstantiatorImpl<>( getEmbeddableJavaTypeDescriptor() );
		}
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer() {
		return reflectionOptimizer;
	}

	@Override
	protected PropertyAccess buildPropertyAccess(Property bootAttributeDescriptor) {
		PropertyAccessStrategy strategy = null;
		final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
		final BuiltInPropertyAccessStrategies namedStrategy = BuiltInPropertyAccessStrategies.interpret(
				propertyAccessorName );
		if ( namedStrategy != null ) {
			strategy = namedStrategy.getStrategy();
		}

		if ( strategy == null ) {

			if ( StringHelper.isNotEmpty( propertyAccessorName ) ) {

				// handle explicitly specified attribute accessor
				strategy = strategySelector.resolveStrategy(
						PropertyAccessStrategy.class,
						propertyAccessorName
				);
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref ) {
					final Backref backref = (Backref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyBackRefImpl( backref.getCollectionRole(), backref
							.getEntityName() );
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref ) {
					final IndexBackref indexBackref = (IndexBackref) bootAttributeDescriptor;
					strategy = new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					strategy = BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}

		if ( strategy == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not resolve PropertyAccess for attribute `%s#%s`",
							getEmbeddableJavaTypeDescriptor().getJavaType().getName(),
							bootAttributeDescriptor.getName()
					)
			);
		}

		return strategy.buildPropertyAccess(
				getEmbeddableJavaTypeDescriptor().getJavaType(),
				bootAttributeDescriptor.getName()
		);
	}

	private ReflectionOptimizer buildReflectionOptimizer(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {

		if ( !Environment.useReflectionOptimizer() ) {
			return null;
		}

		if ( hasCustomAccessors() ) {
			return null;
		}

		final String[] getterNames = new String[getPropertySpan()];
		final String[] setterNames = new String[getPropertySpan()];
		final Class[] propTypes = new Class[getPropertySpan()];

		for ( int i = 0; i < getPropertyAccesses().length; i++ ) {
			final PropertyAccess propertyAccess = getPropertyAccesses()[i];

			getterNames[i] = propertyAccess.getGetter().getMethodName();
			setterNames[i] = propertyAccess.getSetter().getMethodName();
			propTypes[i] = propertyAccess.getGetter().getReturnType();
		}

		return Environment.getBytecodeProvider().getReflectionOptimizer(
				bootDescriptor.getComponentClass(),
				getterNames,
				setterNames,
				propTypes
		);
	}

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.POJO;
	}

	@Override
	public <J> Instantiator<J> getInstantiator() {
		//noinspection unchecked
		return instantiator;
	}
}
