/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.tuple.GenerationTiming;

/**
 * GeneratedValueResolver impl for in-db generation.  It extracts the generated value
 * from a array of the ResultSet values
 *
 * @author Steve Ebersole
 */
@Internal
public class InDatabaseGeneratedValueResolver implements GeneratedValueResolver {
	private final GenerationTiming timing;
	private final int resultPosition;

	public InDatabaseGeneratedValueResolver(GenerationTiming timing, int resultPosition) {
		this.timing = timing;
		this.resultPosition = resultPosition;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public Object resolveGeneratedValue(Object[] row, Object entity, SharedSessionContractImplementor session) {
		return row[resultPosition];
	}
}
