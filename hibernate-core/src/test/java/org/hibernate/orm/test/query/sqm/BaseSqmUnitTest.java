/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.exec.spi.Callback;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmUnitTest
		extends BaseSessionFactoryFunctionalTest
		implements SqlAstCreationContext, Callback {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );

		builder.applySetting( AvailableSettings.JPA_QUERY_COMPLIANCE, strictJpaCompliance() );
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	/**
	 * todo (6.0) : use JUnit parameters for this (see envers)
	 */
	protected boolean strictJpaCompliance() {
		return false;
	}

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
	}

	@Override
	public void invokeAfterLoadActions(Object entity, EntityMappingType entityMappingType, SharedSessionContractImplementor session) {
	}

	@Override
	public boolean hasAfterLoadActions() {
		return false;
	}

	protected SqmSelectStatement<?> interpretSelect(String hql) {
		return interpretSelect( hql, sessionFactory() );
	}

	public static SqmSelectStatement<?> interpretSelect(String hql, SessionFactoryImplementor sessionFactory) {
		return (SqmSelectStatement<?>) sessionFactory
				.getQueryEngine()
				.getHqlTranslator()
				.translate( hql, null );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory();
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return sessionFactory().getRuntimeMetamodels().getMappingMetamodel();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return sessionFactory().getTypeConfiguration();
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return sessionFactory().getJpaMetamodel();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return sessionFactory().getMaximumFetchDepth();
	}
}
