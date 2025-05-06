/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;

/**
 * @author Steve Ebersole
 */
public enum CascadingFetchProfile {
	MERGE,
	REFRESH;

	public CascadingAction<?> getCascadingAction() {
		return switch ( this ) {
			case MERGE -> CascadingActions.MERGE;
			case REFRESH -> CascadingActions.REFRESH;
		};
	}
}
