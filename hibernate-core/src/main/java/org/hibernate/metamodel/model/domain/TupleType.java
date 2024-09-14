/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;

import org.hibernate.query.sqm.SqmExpressible;

/**
 * Describes any structural type without a direct java type representation.
 *
 * @author Christian Beikov
 */
public interface TupleType<J> extends SqmExpressible<J> {

	int componentCount();
	String getComponentName(int index);
	List<String> getComponentNames();

	SqmExpressible<?> get(int index);
	SqmExpressible<?> get(String componentName);
}
