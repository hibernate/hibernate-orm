/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.tuple.TimestampGenerators;
import org.hibernate.tuple.ValueGenerator;

import java.lang.reflect.Member;

import static org.hibernate.annotations.GenerationTime.INSERT;
import static org.hibernate.annotations.GenerationTime.INSERT_OR_UPDATE;

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
public class CurrentTimestampGeneration implements InMemoryGenerator, InDatabaseGenerator {
	private final GenerationTime timing;
	private final ValueGenerator<?> generator;

	public CurrentTimestampGeneration(CurrentTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = annotation.event() == INSERT_OR_UPDATE ? annotation.timing().getEquivalent() : annotation.event();
	}

	public CurrentTimestampGeneration(CreationTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = INSERT;
	}

	public CurrentTimestampGeneration(UpdateTimestamp annotation, Member member, GeneratorCreationContext context) {
		generator = getGenerator( annotation.source(), member );
		timing = INSERT_OR_UPDATE;
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
	public boolean generatedByDatabase() {
		return generator == null;
	}

	@Override
	public boolean generatedOnInsert() {
		return timing.includesInsert();
	}

	@Override
	public boolean generatedOnUpdate() {
		return timing.includesUpdate();
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue) {
		return generator.generateValue( (Session) session, owner, currentValue );
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return true;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.currentTimestamp() };
	}
}
