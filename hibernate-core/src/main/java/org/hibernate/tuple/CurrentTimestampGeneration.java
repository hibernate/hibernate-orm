/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.tuple;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.ReflectHelper;

import java.lang.reflect.Member;

import static org.hibernate.tuple.GenerationTiming.ALWAYS;
import static org.hibernate.tuple.GenerationTiming.INSERT;

/**
 * Value generation strategy which produces a timestamp using the database
 * {@link Dialect#currentTimestamp() current_timestamp} function or the JVM
 * {@linkplain java.time.Clock#instant() current instant}.
 * <p>
 * Underlies the {@link CurrentTimestamp}, {@link CreationTimestamp}, and
 * {@link UpdateTimestamp} annotations.
 *
 * @see CurrentTimestamp
 * @see CreationTimestamp
 * @see UpdateTimestamp
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Internal
public class CurrentTimestampGeneration implements ValueGeneration {
	private final GenerationTiming timing;
	private final ValueGenerator<?> generator;

	public CurrentTimestampGeneration(CurrentTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = annotation.timing();
	}

	public CurrentTimestampGeneration(CreationTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = INSERT;
	}

	public CurrentTimestampGeneration(UpdateTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = ALWAYS;
	}

	private static ValueGenerator<?> getGenerator(SourceType source, Member member) {
		switch (source) {
			case VM:
				// ValueGenerator is only used for in-VM generation
				return TimestampGenerators.get( ReflectHelper.getPropertyType( member ) );
			case DB:
				return null;
			default:
				throw new AssertionFailure("unknown source");
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return generator;
	}

	@Override
	public boolean referenceColumnInSql() {
		return true;
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
