/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results.dynamic;

import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilder extends FetchBuilder, NativeQuery.ReturnProperty {
	DynamicFetchBuilder cacheKeyInstance();

	List<String> getColumnAliases();
}
