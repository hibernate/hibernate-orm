/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import java.util.Collections;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;

/**
 * @author Steve Ebersole
 */
public class SelectByNaturalIdBuilder extends AbstractMetamodelSelectBuilder {
	public SelectByNaturalIdBuilder(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigable) {
		super( sessionFactory, rootNavigable, ( (EntityTypeDescriptor) rootNavigable ).getHierarchy().getNaturalIdDescriptor() );
	}

	@Override
	public EntityTypeDescriptor getRootNavigableContainer() {
		return (EntityTypeDescriptor) super.getRootNavigableContainer();
	}

	@Override
	public SqlAstSelectDescriptor generateSelectStatement(
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		return generateSelectStatement(
				numberOfKeysToLoad,
				Collections.singletonList( getRootNavigableContainer().getHierarchy().getIdentifierDescriptor() ),
				loadQueryInfluencers,
				lockOptions
		);
	}
}
