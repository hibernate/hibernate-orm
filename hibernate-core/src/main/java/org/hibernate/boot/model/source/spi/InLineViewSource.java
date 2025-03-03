/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes in-line view source information.  Generally, either {@link org.hibernate.annotations.Subselect}
 * or {@code <subselect/>}
 *
 * @author Steve Ebersole
 */
public interface InLineViewSource extends TableSpecificationSource {
	/**
	 * Obtain the {@code SQL SELECT} statement to use.  Cannot be null!
	 *
	 * @return The {@code SQL SELECT} statement
	 */
	String getSelectStatement();

	String getLogicalName();
}
