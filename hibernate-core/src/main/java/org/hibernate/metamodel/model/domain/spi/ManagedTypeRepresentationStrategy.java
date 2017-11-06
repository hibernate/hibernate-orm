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
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.property.access.spi.PropertyAccess;

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
	 * todo : do we want these to be entity/component-specific?  Comes down to
	 * whether we want to retain the ability to instantiation an entity passing in
	 * its id-value.  Note however that as currently implemented, even prior top 6.0,
	 * this is completely unnecessary as it is done in 2 steps - first instantiating the
	 * object and then injecting its id value.
	 * <p>
	 * And even if we do want to keep that ability, keep in mind that the JTD being passed
	 * in knows whether the thing is an entity or composite - so the return can already be
	 * entity/composite specific by casting.  E.g., in EntityDescriptor we could simply do:
	 * `EntityInstantiator instantiator = (EntityInstantiator) getRepresentationStrategy().generateInstantiator( getJavaTypeDescriptor() );`
	 */
	<J> Instantiator<J> resolveInstantiator(
			ManagedTypeMapping bootDescriptor,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider);

	/**
	 * note: moving/integrating this with such a representation-specific contract helps to
	 * alleviate some of the awkwardness in `PropertyAccessStrategy` / `PropertyAccessStrategyResolver`
	 *
	 * todo (6.0) : Should we instead be passing in PersistentAttributeMapping?
	 */
	PropertyAccess generatePropertyAccess(
			ManagedTypeMapping bootDescriptor,
			PersistentAttributeMapping bootAttribute,
			ManagedTypeDescriptor runtimeDescriptor,
			BytecodeProvider bytecodeProvider);
}