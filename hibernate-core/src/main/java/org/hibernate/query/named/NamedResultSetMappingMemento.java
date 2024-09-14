/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultSetMapping;

/**
 * Used to keep information about named result mappings defined by the
 * application which can then be applied to native-sql and stored-procedure
 * queries.
 *
 * These can be defined in a number of ways:<ul>
 *     <li>{@link jakarta.persistence.SqlResultSetMapping}</li>
 *     <li>JPA Class-based mapping</li>
 *     <li>Hibernate's legacy XML-defined mapping</li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Incubating
public interface NamedResultSetMappingMemento {
	/**
	 * The name associated with this memento
	 */
	String getName();

	/**
	 * Resolve this memento.  Generally speaking, this involves building ResultBuilder instances for
	 * each defined result and registering them with the passed `resultSetMapping`.  Any known
	 * query spaces should be passed to the `querySpaceConsumer`
	 */
	void resolve(
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
