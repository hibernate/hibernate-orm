/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.RepresentationStrategy;
import org.hibernate.metamodel.model.domain.spi.RepresentationStrategySelector;

/**
 * @author Steve Ebersole
 */
public class StandardRepresentationStrategySelector implements RepresentationStrategySelector {
	/**
	 * Singleton access
	 */
	public static final StandardRepresentationStrategySelector INSTANCE = new StandardRepresentationStrategySelector();

	@Override
	public RepresentationStrategy resolveRepresentationStrategy(
			ManagedTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		// todo (6.0) : allow this selector to be Object to allow for custom RepresentationStrategy impls
		RepresentationMode representation = bootMapping.getExplicitRepresentationMode();
		if ( representation == null ) {
			if ( bootMapping.getJavaTypeDescriptor().getJavaType() == null ) {
				representation = RepresentationMode.MAP;
			}
			else {
				representation = RepresentationMode.POJO;
			}
		}

		if ( representation == RepresentationMode.MAP ) {
			return StandardMapRepresentationStrategy.INSTANCE;
		}
		else {
			// todo (6.0) : fix this
			// 		currently we end up resolving the ReflectionOptimizer from the BytecodeProvider
			//		multiple times per class
			//
			//		instead, resolve ReflectionOptimizer once - here - and pass along to
			//		StandardPojoRepresentationStrategy
			return new StandardPojoRepresentationStrategy( bootMapping, creationContext );
		}
	}
}
