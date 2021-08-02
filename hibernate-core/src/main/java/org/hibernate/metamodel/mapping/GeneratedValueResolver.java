/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.NoGeneratedValueResolver;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGeneration;

/**
 * Generalized contract covering an attribute's generation handling
 *
 * @author Steve Ebersole
 */
@Incubating
public interface GeneratedValueResolver {
	static GeneratedValueResolver from(
			ValueGeneration valueGeneration,
			GenerationTiming requestedTiming,
			int dbSelectionPosition) {
		assert requestedTiming != GenerationTiming.NEVER;

		if ( valueGeneration == null || valueGeneration.getGenerationTiming().includes( GenerationTiming.NEVER ) ) {
			return NoGeneratedValueResolver.INSTANCE;
		}

		if ( requestedTiming == GenerationTiming.ALWAYS && valueGeneration.getGenerationTiming() == GenerationTiming.INSERT ) {
			return NoGeneratedValueResolver.INSTANCE;
		}

		// todo (6.x) : incorporate `org.hibernate.tuple.InDatabaseValueGenerationStrategy`
		// 		and `org.hibernate.tuple.InMemoryValueGenerationStrategy` from `EntityMetamodel`.
		//		this requires unification of the read and write (insert/update) aspects of
		//		value generation which we'll circle back to as we convert write operations to
		//		use the "runtime mapping" (`org.hibernate.metamodel.mapping`) model

		if ( valueGeneration.getValueGenerator() == null ) {
			// in-db generation (column-default, function, etc)
			return new InDatabaseGeneratedValueResolver( requestedTiming, dbSelectionPosition );
		}

		return new InMemoryGeneratedValueResolver( valueGeneration.getValueGenerator(), requestedTiming );
	}

	GenerationTiming getGenerationTiming();
	Object resolveGeneratedValue(Object[] row, Object entity, SharedSessionContractImplementor session);
}
