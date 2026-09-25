package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchTiming;

/**
 * Access to a FetchTiming
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface FetchTimingAccess {
	FetchTiming getTiming();
}
