package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchStyle;

/**
 * Access to a FetchStyle
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface FetchStyleAccess {
	FetchStyle getStyle();
}
