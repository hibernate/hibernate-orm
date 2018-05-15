/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;

/**
 * @author Andrea Boriero
 */
public class SelectByUniqueKeyBuilder extends AbstractMetamodelSelectBuilder {

	public SelectByUniqueKeyBuilder(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigable,
			Navigable restrictedNavigable) {
		super( sessionFactory, rootNavigable, restrictedNavigable );
	}
}
