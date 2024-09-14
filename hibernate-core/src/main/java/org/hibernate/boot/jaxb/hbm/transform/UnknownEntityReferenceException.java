/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.Origin;

/**
 * We encountered a reference to an entity by name which we cannot resolve
 *
 * @author Steve Ebersole
 */
public class UnknownEntityReferenceException extends MappingException {
	public UnknownEntityReferenceException(String name, Origin origin) {
		super( String.format(
				Locale.ROOT,
				"Could not resolve entity name `%s` : %s (%s)",
				name,
				origin.getName(),
				origin.getType()
		) );
	}
}
