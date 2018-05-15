/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import java.util.Set;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.QuerySplitter;

/**
 * A specialized EntityValuedExpressableType for cases where the entity reference named
 * an "unmapped polymorphic" class.  E.g. given a query like
 * {@code select o from java.lang.Object o} we would ask the consumer for
 * an EntityValuedExpressableType for {@code java.lang.Object} and the consumer
 * would return us {@code PolymorphicEntityValuedExpressableType<java.lang.Object>}
 * which would contain all mapped entities as its {@link #getImplementors()}.
 * <p/>
 * Such "unmapped polymorphic" references are only valid in the root from-clause
 * and only one such reference is allow for the query.
 *
 * @author Steve Ebersole
 *
 * @see QuerySplitter
 */
public interface PolymorphicEntityValuedExpressableType<T> extends EntityValuedExpressableType<T> {

	// todo (6.0) : should producing an SQM just automatically pass the tree through QuerySplitter?
	//		that works for ORM usage, but what about others (OGM, Search, etc)?  There is an overhead
	//		in performing that call (partial tree walking) which would be good to avoid if
	//		the consumer (caller) does not support unmapped-polymorphic queries.

	/**
	 * Access to the specific "concrete" implementors of the
	 * unmapped polymorphism modeled here.
	 *
	 * @return All concrete implementors.
	 */
	Set<EntityTypeDescriptor<?>> getImplementors();
}
