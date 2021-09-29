/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.intg.sqm;

import java.util.Collections;
import java.util.Optional;

import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.orm.test.jpa.JpaComplianceStub;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.internal.NamedObjectRepositoryImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests making sure that HQL queries can fully be translated without a SessionFactory.
 *
 * todo (6.0) : have a way to directly load these translations into the {@link NamedObjectRepository}
 * 		directly.  For example Quarkus could store its build-time translations and directly here during boot-strap of the SF
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.RETAIL )
public class HqlTranslationNoFactoryTests {
	@Test
	@FailureExpected( reason = "Building the JpaDomain")
	public void testHqlTranslationNoSessionFactory(DomainModelScope modelScope, ServiceRegistryScope registryScope) {
		final String hql = "select a from SalesAssociate a";

		final HqlTranslator hqlTranslator = buildHqlTranslator( modelScope, registryScope );
		final SqmStatement<?> sqmStatement = hqlTranslator.translate( hql );
		assert sqmStatement != null;
	}

	private HqlTranslator buildHqlTranslator(DomainModelScope modelScope, ServiceRegistryScope registryScope) {
		final MetadataImplementor bootModel = modelScope.getDomainModel();
		final TypeConfiguration typeConfiguration = bootModel.getTypeConfiguration();

		final JpaMetamodelImpl jpaMetamodel = new JpaMetamodelImpl( typeConfiguration, new JpaComplianceStub() );

		// todo (6.0) (quarkus) : we should limit the type of the last argument here from `RuntimeModelCreationContext`
		//  		which assumes access to SessionFactory
		jpaMetamodel.processJpa(
				bootModel,
				Collections.emptyMap(),
				JpaStaticMetaModelPopulationSetting.ENABLED,
				Collections.emptyList(),
				new RuntimeModelCreationContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						throw new UnsupportedOperationException( "SessionFactory not available" );
					}

					@Override
					public BootstrapContext getBootstrapContext() {
						return typeConfiguration.getMetadataBuildingContext().getBootstrapContext();
					}

					@Override
					public MetadataImplementor getBootModel() {
						return modelScope.getDomainModel();
					}

					@Override
					public MappingMetamodel getDomainModel() {
						throw new UnsupportedOperationException( "DomainMetamodel not available" );
					}
				}
		);

		Optional<QueryEngine> queryEngineAccess = Optional.empty();

		// todo (6.0) (quarkus) : this circularity is problematic as well
		//		between `SqmCreationContext#getQueryEngine` and the `SqmCreationContext` passed to `#QueryEngine`
		final SqmCreationContext sqmCreationContext = new SqmCreationContext() {
			@Override
			public JpaMetamodel getJpaMetamodel() {
				return jpaMetamodel;
			}

			@Override
			public QueryEngine getQueryEngine() {
				return queryEngineAccess.orElseThrow( () -> new RuntimeException( "Unexpected access to `SqmCreationContext#getQueryEngine`" ) );
			}
		};

		// we don't want strict JPA query compliance
		final SqmCreationOptions sqmCreationOptions = () -> false;

		final QueryEngine queryEngine = new QueryEngine(
				null,
				null,
				jpaMetamodel,
				ValueHandlingMode.BIND,
				ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( registryScope.getRegistry() ),
				// we don't want strict JPA query compliance
				false,
				new NamedObjectRepositoryImpl( Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap() ),
				// NativeQueryInterpreter
				null,
				// this is exclusively to build the SqmFunctionRegistry, maybe Quarkus should just build it directly and pass
				registryScope.getRegistry().getService( JdbcServices.class ).getDialect(),
				registryScope.getRegistry()
		);

		return new StandardHqlTranslator(
				new SqmCreationContext() {
					@Override
					public JpaMetamodel getJpaMetamodel() {
						return jpaMetamodel;
					}

					@Override
					public QueryEngine getQueryEngine() {
						return queryEngine;
					}
				},
				sqmCreationOptions
		);
	}

}
