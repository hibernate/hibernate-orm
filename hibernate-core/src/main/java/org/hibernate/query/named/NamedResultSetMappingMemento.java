/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named;

import org.hibernate.Incubating;
import org.hibernate.query.spi.ResultSetMapping;

/**
 * Used to keep information about named result mappings defined by the
 * application which can then be applied to native-sql and stored-procedure
 * queries.
 *
 * These can be defined in a number of ways:<ul>
 *     <li>{@link javax.persistence.SqlResultSetMapping}</li>
 *     <li>JPA Class-based mapping</li>
 *     <li>Hibernate's legacy XML-defined mapping</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Incubating
public interface NamedResultSetMappingMemento {
	String getName();

	/**
	 * todo (6.0) : determine the proper arguments.  depends on how we port JdbcValues, etc from the original 6.0 work
	 */
	ResultSetMapping toResultSetMapping();
}
