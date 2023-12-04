/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface SecondPass {
	boolean process();
}
