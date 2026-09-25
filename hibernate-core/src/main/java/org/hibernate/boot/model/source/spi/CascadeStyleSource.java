package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Describes sources which define cascading.
 *
 * @author Steve Ebersole
 */
@Remove
public interface CascadeStyleSource {
	/**
	 * Obtain the cascade styles to be applied to this association.
	 *
	 * @return The cascade styles.
	 */
	String getCascadeStyleName();
}
