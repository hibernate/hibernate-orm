/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

import org.hibernate.query.SemanticException;

/**
 * Indicates a failure to resolve an entity name in HQL to a known mapped
 * entity type.
 *
 * @apiNote The JPA criteria API requires that this problem be reported
 *          as an {@link IllegalArgumentException}, and so we usually
 *          throw {@link EntityTypeException} from the SQM objects, and
 *          then wrap as an instance of this exception type in the
 *          {@link org.hibernate.query.hql.HqlTranslator}.
 *
 * @author Steve Ebersole
 *
 * @see EntityTypeException
 */
public class UnknownEntityException extends SemanticException {
	private final String entityName;

	public UnknownEntityException(String entityName) {
		this( "Could not resolve entity '" + entityName + "'", entityName );
	}

	public UnknownEntityException(String message, String entityName) {
		super( message );
		this.entityName = entityName;
	}

	public UnknownEntityException(String message, String entityName, Exception cause) {
		super( message, cause );
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}
}
