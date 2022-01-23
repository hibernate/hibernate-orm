/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Additional contract describing the source of an identifier mapping whose {@linkplain #getNature() nature} is
 * {@linkplain org.hibernate.id.EntityIdentifierNature#SIMPLE simple}.
 *
 * @author Steve Ebersole
 */
public interface IdentifierSourceSimple extends IdentifierSource {
	/**
	 * Obtain the source descriptor for the identifier attribute.
	 *
	 * @return The identifier attribute source.
	 */
	SingularAttributeSource getIdentifierAttributeSource();

	/**
	 *  Returns the "unsaved" entity identifier value.
	 *
	 *  @return the "unsaved" entity identifier value
	 */
	String getUnsavedValue();

}
