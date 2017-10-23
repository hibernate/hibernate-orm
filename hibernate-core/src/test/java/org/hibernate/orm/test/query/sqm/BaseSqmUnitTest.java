/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.orm.test.BaseUnitTest;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmUnitTest
		extends BaseUnitTest
		implements SqlAstBuildingContext, Callback {

	@Override
	public Callback getCallback() {
		return this;
	}

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
	}

	protected SqmSelectStatement interpretSelect(String hql) {
		return (SqmSelectStatement) getSessionFactory().getQueryEngine().getSemanticQueryProducer().interpret( hql );
	}
}
