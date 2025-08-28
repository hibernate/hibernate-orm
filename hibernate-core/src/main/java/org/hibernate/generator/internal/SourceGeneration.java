/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.Internal;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.type.descriptor.java.JavaType;


import java.lang.reflect.Member;
import java.util.EnumSet;

import static org.hibernate.generator.internal.CurrentTimestampGeneration.getCurrentTimestamp;

/**
 * Value generation strategy using the query {@link Dialect#getCurrentTimestampSelectString()}.
 * This is a {@code select} that occurs <em>before</em> the {@code insert} or {@code update},
 * whereas with {@link CurrentTimestampGeneration} the {@code select} happens afterward.
 * <p>
 * Underlies the {@link Source @Source} annotation, and {@code <timestamp source="db"/>} in
 * {@code hbm.xml} mapping documents.
 *
 * @see Source
 * @see CurrentTimestampGeneration
 *
 * @author Gavin King
 *
 * @deprecated because both {@link Source} and {@code hbm.xml} are deprecated, though this
 *             implementation is instructive
 */
@Deprecated(since = "6.2")
@Internal
public class SourceGeneration implements BeforeExecutionGenerator {

	private final JavaType<?> propertyType;
	private final CurrentTimestampGeneration.GeneratorDelegate valueGenerator;

	public SourceGeneration(Source annotation, Member member, GeneratorCreationContext context) {
		this( annotation.value(), context.getProperty().getType().getReturnedClass(), context );
	}

	public SourceGeneration(SourceType sourceType, Class<?> propertyType, GeneratorCreationContext context) {
		this.propertyType = context.getDatabase().getTypeConfiguration().getJavaTypeRegistry().getDescriptor( propertyType );
		this.valueGenerator = CurrentTimestampGeneration.getGeneratorDelegate( sourceType, propertyType, context );
	}

	/**
	 * @return {@link EventTypeSets#ALL}
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.ALL;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		return valueGenerator == null
				? propertyType.wrap( getCurrentTimestamp( session ), session )
				: valueGenerator.generate();
	}
}
