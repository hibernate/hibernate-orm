/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

/**
 * @author Steve Ebersole
 */
public enum DowncastLocation {
	FROM,
	SELECT,
	WHERE,
	OTHER
}
