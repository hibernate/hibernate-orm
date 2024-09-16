/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

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
