/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Defines a singular extension point for capabilities pertaining to
 * a representation mode.  Acts as a factory for delegates encapsulating
 * these capabilities.
 *
 * While building the boot or runtime model, we would resolve the RepresentationStrategy
 * to use for a particular Navigable tree and use it to build the representation-specific
 * components - `Instantiator` and `PropertyAccess`.
 *
 * Could also potentially allow custom RepresentationStrategy impls (and expose
 * ala `@Tuplizer`
 *
 * @apiNote Generally speaking a RepresentationStrategy would correspond
 * to a {@link RepresentationMode} value,
 * however applications are allowed to specify custom strategies as well.
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeRepresentationStrategy {
	RepresentationMode getMode();

	/**
	 * Create a delegate capable of instantiating instances of the specified
	 * managed type.
	 *
	 * @param bootDescriptor The boot-model descriptor of the type top be instantiated
	 * @param runtimeDescriptor The (in-flight) runtime-model descriptor of the type top be instantiated
	 * @param bytecodeProvider The effective BytecodeProvider - can integrate with the
	 * {@link org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer} provided by the
	 * provider's {@linkplain BytecodeProvider#getReflectionOptimizer ReflectionOptimizer}
	 */
	<J> Instantiator<J> resolveInstantiator(
			ManagedTypeMapping bootDescriptor,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider);

	/**
	 * Create the delegate capable of producing proxies for the given entity
	 */
	<J> ProxyFactory generateProxyFactory(
			AbstractEntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext);

	/**
	 * @apiNote Moving/integrating this with such a representation-specific contract
	 * helps to alleviate some of the awkwardness in `PropertyAccessStrategy` /
	 * `PropertyAccessStrategyResolver`
	 */
	PropertyAccess generatePropertyAccess(
			ManagedTypeMapping bootDescriptor,
			PersistentAttributeMapping bootAttribute,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider);
}
