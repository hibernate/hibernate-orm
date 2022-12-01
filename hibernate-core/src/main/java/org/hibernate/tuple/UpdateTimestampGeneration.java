/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;

/**
 * Value generation implementation for {@link UpdateTimestamp}, no longer used.
 *
 * @author Gunnar Morling
 *
 * @deprecated use {@link CurrentTimestampGeneration}
 */
@Internal
@Deprecated(since = "6.2") @Remove
public class UpdateTimestampGeneration implements AnnotationValueGeneration<UpdateTimestamp> {

	private ValueGenerator<?> generator;

	@Override
	public void initialize(UpdateTimestamp annotation, Class<?> propertyType) {
		if ( annotation.source() == SourceType.VM ) {
			generator = TimestampGenerators.get( propertyType );
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.ALWAYS;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return generator;
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return "current_timestamp";
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue(Dialect dialect) {
		return dialect.currentTimestamp();
	}
}
