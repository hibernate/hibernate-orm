/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchStyle;

/**
 * Access to a FetchStyle
 *
 * @author Steve Ebersole
 */
public interface FetchStyleAccess {
	FetchStyle getStyle();
}
