/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.async;

/**
 * Something we want to perform with protection to time it out.
 *
 * @author Steve Ebersole
 */
public interface Executable {
	/**
	 * Perform the action
	 */
	public void execute();

	/**
	 * Called when the timeout period is exceeded.
	 */
	public void timedOut();
}
