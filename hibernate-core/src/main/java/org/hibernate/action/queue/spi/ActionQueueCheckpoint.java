/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;

/// An opaque checkpoint of an [ActionQueue]'s state before speculative flush
/// preparation.
///
/// A checkpoint belongs to the queue instance which created it.  Callers may
/// retain it only long enough to either continue with execution or restore that
/// queue after deciding that the speculative flush is unnecessary.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface ActionQueueCheckpoint {
}
