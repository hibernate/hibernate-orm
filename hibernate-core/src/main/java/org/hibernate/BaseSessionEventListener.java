/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * A noop implementation of {@link SessionEventListener}.
 * Intended as a convenient base class for developing
 * {@code SessionEventListener} implementations.
 *
 * @author Steve Ebersole
 *
 * @deprecated Just implement {@link SessionEventListener} directly.
 */
@Deprecated(since = "7", forRemoval = true)
public class BaseSessionEventListener implements SessionEventListener {
}
