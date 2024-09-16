/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * @author Christian Beikov
 */
public enum FrameExclusion {
	CURRENT_ROW,
	GROUP,
	TIES,
	NO_OTHERS
}
