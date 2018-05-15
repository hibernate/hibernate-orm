/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;

/**
 * A metamodel-driven builder of SqlAstSelectDescriptor
 *
 * @author Steve Ebersole
 */
public interface MetamodelSelectBuilder {
	NavigableContainer getRootNavigableContainer();

	SqlAstSelectDescriptor generateSelectStatement(
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions);
}
