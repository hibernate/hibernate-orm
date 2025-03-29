/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.common;

/**
 * @author Christian Beikov
 */
public enum FrameKind {
	UNBOUNDED_PRECEDING,
	OFFSET_PRECEDING,
	CURRENT_ROW,
	OFFSET_FOLLOWING,
	UNBOUNDED_FOLLOWING
}
