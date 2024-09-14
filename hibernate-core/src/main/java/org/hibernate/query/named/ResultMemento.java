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
import org.hibernate.query.results.ResultBuilder;

/**
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultMemento extends ResultMappingMementoNode {
	ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);
}
