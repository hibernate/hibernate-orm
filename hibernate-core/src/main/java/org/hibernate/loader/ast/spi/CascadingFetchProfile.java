package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;

/**
 * @author Steve Ebersole
 */
public enum CascadingFetchProfile {
	MERGE,
	REFRESH;

	@Nonnull
	public CascadingAction<?> getCascadingAction() {
		return switch ( this ) {
			case MERGE -> CascadingActions.MERGE;
			case REFRESH -> CascadingActions.REFRESH;
		};
	}
}
