/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.common;

/**
 * @author Christian Beikov
 */
public enum FrameExclusion {
	CURRENT_ROW,
	GROUP,
	TIES,
	NO_OTHERS
}
