/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
