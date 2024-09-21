/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result;

/**
 * Models a return that is an update count (count of rows affected)
 *
 * @author Steve Ebersole
 */
public interface UpdateCountOutput extends Output {
	/**
	 * Retrieve the associated update count
	 *
	 * @return The update count
	 */
	int getUpdateCount();
}
