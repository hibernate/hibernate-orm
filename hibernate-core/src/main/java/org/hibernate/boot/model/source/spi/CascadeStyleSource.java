/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes sources which define cascading.
 *
 * @author Steve Ebersole
 */
public interface CascadeStyleSource {
	/**
	 * Obtain the cascade styles to be applied to this association.
	 *
	 * @return The cascade styles.
	 */
	String getCascadeStyleName();
}
