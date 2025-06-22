/*
 * SPDX-License-Identifier: Apache-2.0
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
