/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Locale;
import javax.persistence.EntityNotFoundException;

/**
 * Exception for {@link org.hibernate.annotations.NotFoundAction#EXCEPTION}
 *
 * @see org.hibernate.annotations.NotFound
 *
 * @author Steve Ebersole
 */
public class FetchNotFoundException extends EntityNotFoundException {
	private final String entityName;
	private final Object identifier;

	public FetchNotFoundException(String entityName, Object identifier) {
		super(
				String.format(
						Locale.ROOT,
						"Entity `%s` with identifier value `%s` does not exist",
						entityName,
						identifier
				)
		);
		this.entityName = entityName;
		this.identifier = identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getIdentifier() {
		return identifier;
	}
}
