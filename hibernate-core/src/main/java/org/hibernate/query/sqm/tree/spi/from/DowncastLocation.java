package org.hibernate.query.sqm.tree.spi.from;

/**
 * @author Steve Ebersole
 */
public enum DowncastLocation {
	FROM,
	SELECT,
	WHERE,
	OTHER
}
