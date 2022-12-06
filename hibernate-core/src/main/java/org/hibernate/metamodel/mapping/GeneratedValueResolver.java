/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Generalized contract covering an attribute's generation handling
 *
 * @author Steve Ebersole
 */
@Incubating
public interface GeneratedValueResolver {
//	static GeneratedValueResolver from(
//			Generator generator,
//			GenerationTiming requestedTiming,
//			int dbSelectionPosition) {
//		assert requestedTiming.isNotNever();
//
//		if ( generator == null || !generator.getGenerationTiming().includes( requestedTiming ) ) {
//			return NoGeneratedValueResolver.INSTANCE;
//		}
//		else {
//			return generator.generatedByDatabase()
//					? new InDatabaseGeneratedValueResolver( requestedTiming, dbSelectionPosition ) // in-db generation (column-default, function, etc)
//					: new InMemoryGeneratedValueResolver( (InMemoryGenerator) generator, requestedTiming );
//		}
//	}

//	GenerationTiming getGenerationTiming();
	Object resolveGeneratedValue(Object[] row, Object entity, SharedSessionContractImplementor session, Object currentValue);
}
