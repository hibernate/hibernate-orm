/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

import org.junit.After;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
public abstract class BaseUnitTest
		extends org.hibernate.testing.junit4.BaseUnitTestCase
		implements SqlAstBuildingContext, Callback {
	private SessionFactoryImplementor sessionFactory;

	@Before
	public void before() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.JPAQL_STRICT_COMPLIANCE, strictJpaCompliance() )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "none" )
				.build();

		try {
			MetadataSources metadataSources = new MetadataSources( ssr );
			applyMetadataSources( metadataSources );

			this.sessionFactory = (SessionFactoryImplementor) metadataSources.buildMetadata().buildSessionFactory();
		}
		catch (Exception e) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}

	protected boolean strictJpaCompliance() {
		return false;
	}

	protected void applyMetadataSources(MetadataSources metadataSources) {
	}

	@After
	public void after() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Override
	public final SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

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
