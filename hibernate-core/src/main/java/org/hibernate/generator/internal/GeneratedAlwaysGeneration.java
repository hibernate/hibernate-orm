/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;

import java.util.EnumSet;

import static org.hibernate.generator.EventTypeSets.ALL;

/**
 * For {@link GeneratedColumn}.
 *
 * @author Gavin King
 */
public class GeneratedAlwaysGeneration implements OnExecutionGenerator {
	private Class<?> generatedType;

	public GeneratedAlwaysGeneration() {}

	public GeneratedAlwaysGeneration(GeneratorCreationContext context) {
		generatedType = context.getType().getReturnedClass();
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return ALL;
	}

	@Override
	public Class<?> getGeneratedType() {
		return generatedType;
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return null;
	}
}
