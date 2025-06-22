/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.lang.reflect.Member;
import java.util.EnumSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.util.StringHelper;

/**
 * Proposal for making `@GeneratedValueGeneration` work for update (they don't work in 5.x either)
 *
 * @see ProposedGenerated
 *
 * @author Steve Ebersole
 */
public class ProposedGeneratedValueGeneration implements OnExecutionGenerator {
	private final EnumSet<EventType> timing;
	private final String defaultValue;

	public ProposedGeneratedValueGeneration(ProposedGenerated annotation, Member member, GeneratorCreationContext context) {
		timing = EventTypeSets.fromArray( annotation.timing() );
		defaultValue = StringHelper.nullIfEmpty( annotation.sqlDefaultValue() );
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return timing;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return defaultValue != null;
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { defaultValue };
	}
}
