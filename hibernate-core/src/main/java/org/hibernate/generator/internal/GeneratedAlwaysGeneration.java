/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;

import java.util.EnumSet;

import static org.hibernate.generator.EventTypeSets.ALL;

/**
 * For {@link GeneratedColumn}.
 *
 * @author Gavin King
 */
public class GeneratedAlwaysGeneration implements OnExecutionGenerator {

	public GeneratedAlwaysGeneration() {}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return ALL;
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
