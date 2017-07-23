/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sql.spi.ReturnableResultNodeImplementor;

/**
 * Keep a description of the {@link javax.persistence.SqlResultSetMapping}
 *
 * Note that we do not track joins/fetches here as we do in the manual approach
 * as this feature is defined by JPA and we simply support the JPA feature here.
 *
 * Note also that we track the result builders here (as opposed to the
 * QueryResult) to better fit with {@link org.hibernate.query.NativeQuery}
 * which is where this is ultimately used.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class ResultSetMappingDefinition {
	private final String name;
	private final List<ReturnableResultNodeImplementor> queryReturns = new ArrayList<>();

	/**
	 * Constructs a ResultSetMappingDefinition with name
	 */
	public ResultSetMappingDefinition(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Adds a return.
	 *
	 * @param queryReturn The return
	 */
	public void addQueryReturn(ReturnableResultNodeImplementor queryReturn) {
		queryReturns.add( queryReturn );
	}

	public List<ReturnableResultNodeImplementor> getQueryReturns() {
		return Collections.unmodifiableList( queryReturns );
	}
}
