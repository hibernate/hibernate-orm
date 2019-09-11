/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;


import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class StandardManagedTypeRepresentationResolver implements ManagedTypeRepresentationResolver {
	/**
	 * Singleton access
	 */
	public static final StandardManagedTypeRepresentationResolver INSTANCE = new StandardManagedTypeRepresentationResolver();

	@Override
	public EntityRepresentationStrategy resolveStrategy(
			PersistentClass bootDescriptor,
			EntityPersister runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {
//		RepresentationMode representation = bootDescriptor.getExplicitRepresentationMode();
		RepresentationMode representation = null;
		if ( representation == null ) {
			if ( bootDescriptor.getMappedClass() == null ) {
				representation = RepresentationMode.MAP;
			}
			else {
				representation = RepresentationMode.POJO;
			}
		}

		if ( representation == RepresentationMode.MAP ) {
			return new StandardMapEntityRepresentationStrategy( bootDescriptor, creationContext );
		}
		else {
			// todo (6.0) : fix this
			// 		currently we end up resolving the ReflectionOptimizer from the BytecodeProvider
			//		multiple times per class
			//
			//		instead, resolve ReflectionOptimizer once - here - and pass along to
			//		StandardPojoRepresentationStrategy
			return new StandardPojoEntityRepresentationStrategy( bootDescriptor, creationContext );
		}
	}

	@Override
	public EmbeddableRepresentationStrategy resolveStrategy(
			Component bootDescriptor,
			RuntimeModelCreationContext creationContext) {
//		RepresentationMode representation = bootDescriptor.getExplicitRepresentationMode();
		RepresentationMode representation = null;
		if ( representation == null ) {
			if ( bootDescriptor.getComponentClass() == null ) {
				representation = RepresentationMode.MAP;
			}
			else {
				representation = RepresentationMode.POJO;
			}
		}

		if ( representation == RepresentationMode.MAP ) {
			return new StandardMapEmbeddableRepresentationStrategy( bootDescriptor, creationContext );
		}
		else {
			// todo (6.0) : fix this
			// 		currently we end up resolving the ReflectionOptimizer from the BytecodeProvider
			//		multiple times per class
			//
			//		instead, resolve ReflectionOptimizer once - here - and pass along to
			//		StandardPojoRepresentationStrategy
			return new StandardPojoEmbeddableRepresentationStrategy( bootDescriptor, creationContext );
		}
	}
}
