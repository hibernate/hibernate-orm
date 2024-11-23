/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * @author Steve Ebersole
 */
@Internal
public interface RestrictionRenderingContext {
	String makeParameterMarker();
}
