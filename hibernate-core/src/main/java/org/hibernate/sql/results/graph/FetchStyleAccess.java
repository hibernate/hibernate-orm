/*
 * SPDX-License-Identifier: Apache-2.0
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
