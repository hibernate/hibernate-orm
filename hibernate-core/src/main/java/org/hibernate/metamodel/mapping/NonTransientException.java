/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Marker interface for exceptions thrown during mapping-model creation which
 * are not transient errors - they will never succeed
 *
 * @author Steve Ebersole
 */
public interface NonTransientException {
}
