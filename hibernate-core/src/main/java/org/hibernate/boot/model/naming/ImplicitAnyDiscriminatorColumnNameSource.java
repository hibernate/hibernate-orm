/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name for an ANY mapping's discriminator
 * column.  Historically the ANY discriminator column name had to be specified.
 *
 * @author Steve Ebersole
 */
public interface ImplicitAnyDiscriminatorColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the AttributePath to the ANY mapping
	 *
	 * @return The AttributePath to the ANY mapping
	 */
	AttributePath getAttributePath();
}
