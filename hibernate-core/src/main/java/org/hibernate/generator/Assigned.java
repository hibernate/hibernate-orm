/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.EnumSet;

/**
 * A {@link Generator} that doesn't generate.
 * <p>
 * Identifier fields with an {@code Assigned} generator
 * must be assigned a value by the application program.
 * <p>
 * This replaces the {@link org.hibernate.id.Assigned}
 * identifier generation strategy from older versions
 * of Hibernate.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
@Internal
public class Assigned implements Generator {
	@Override
	public boolean generatedOnExecution() {
		return false;
	}

	@Override
	public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}

	@Override
	public boolean allowMutation() {
		return true;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.NONE;
	}

	@Override
	public boolean generatesSometimes() {
		return false;
	}

	@Override
	public boolean generatesOnInsert() {
		return false;
	}

	@Override
	public boolean generatesOnUpdate() {
		return false;
	}
}
