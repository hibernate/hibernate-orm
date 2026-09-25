package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * @author Steve Ebersole
 */
@Remove
public interface FetchCharacteristics {
	FetchTiming getFetchTiming();
	FetchStyle getFetchStyle();
}
