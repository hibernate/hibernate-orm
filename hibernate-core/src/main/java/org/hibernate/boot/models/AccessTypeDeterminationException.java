/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ClassDetails;

/**
 * Indicates that {@link jakarta.persistence.AccessType} could not be
 * determined
 *
 * @author Steve Ebersole
 */
public class AccessTypeDeterminationException extends MappingException {
	public AccessTypeDeterminationException(ClassDetails managedClass) {
		super(
				String.format(
						Locale.ROOT,
						"Unable to determine default `AccessType` for class `%s`",
						managedClass.getName()
				)
		);
	}
}
