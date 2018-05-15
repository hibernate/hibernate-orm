/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.ProxyFactory;

/**
 * @author Steve Ebersole
 */
public class StandardMapRepresentationStrategy implements ManagedTypeRepresentationStrategy {
	/**
	 * Singleton access
	 */
	public static final StandardMapRepresentationStrategy INSTANCE = new StandardMapRepresentationStrategy();

	@Override
	public RepresentationMode getMode() {
		return RepresentationMode.MAP;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Instantiator resolveInstantiator(
			ManagedTypeMapping bootModel,
			ManagedTypeDescriptor runtimeModel,
			BytecodeProvider bytecodeProvider) {
		return new DynamicMapInstantiator( runtimeModel.getNavigableRole() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <J> ProxyFactory generateProxyFactory(
			AbstractEntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
		return StandardMapProxyFactoryInstantiator.INSTANCE.instantiate( runtimeDescriptor, creationContext );
	}

	@Override
	public PropertyAccess generatePropertyAccess(
			ManagedTypeMapping bootDescriptor,
			PersistentAttributeMapping bootAttribute,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider) {
		return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null,
				bootAttribute.getName()
		);
	}
}
