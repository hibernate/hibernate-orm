/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.Collections;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.JpaCompliance;

import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class JpaComplianceTests {
	@Test
	public void testDefaultTrue() {
		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), true );
		assertAll( compliance, true );
	}

	@Test
	public void testDefaultFalse() {
		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), false );
		assertAll( compliance, false );
	}

	private void assertAll(JpaCompliance compliance, boolean expected) {
		assertThat( compliance.isJpaQueryComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaTransactionComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaClosedComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaOrderByMappingComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaProxyComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaCacheComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isGlobalGeneratorScopeEnabled() ).isEqualTo( expected );
	}
	@Test
	public void testDefaultTrueWithOverride() {
		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), true );
		compliance.setQueryCompliance( false );
		assertOverridden( compliance, true );
	}

	@Test
	public void testDefaultFalseWithOverride() {
		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), false );
		compliance.setQueryCompliance( true );
		assertOverridden( compliance, false );
	}

	private void assertOverridden(MutableJpaComplianceImpl compliance, boolean expected) {
		assertThat( compliance.isJpaQueryComplianceEnabled() ).isEqualTo( !expected );
		assertThat( compliance.isJpaTransactionComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaClosedComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaOrderByMappingComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaProxyComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isJpaCacheComplianceEnabled() ).isEqualTo( expected );
		assertThat( compliance.isGlobalGeneratorScopeEnabled() ).isEqualTo( expected );
	}

	@Test
	public void testSettingTrue() {
		ServiceRegistryScope.using(
				() -> ServiceRegistryUtil.serviceRegistryBuilder()
						.applySetting( AvailableSettings.JPA_COMPLIANCE, true )
						.build(),
				(serviceRegistryScope) -> {
					final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( serviceRegistryScope.getRegistry() )
							.buildMetadata()
							.buildSessionFactory();
					final JpaCompliance jpaCompliance = sessionFactory.getSessionFactoryOptions().getJpaCompliance();
					assertAll( jpaCompliance, true );
				}
		);
		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), true );
		assertAll( compliance, true );
	}

	@Test
	public void testSettingFalse() {
		ServiceRegistryScope.using(
				() -> ServiceRegistryUtil.serviceRegistryBuilder()
						.applySetting( AvailableSettings.JPA_COMPLIANCE, false )
						.build(),
				(serviceRegistryScope) -> {
					final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( serviceRegistryScope.getRegistry() )
							.buildMetadata()
							.buildSessionFactory();
					final JpaCompliance jpaCompliance = sessionFactory.getSessionFactoryOptions().getJpaCompliance();
					assertAll( jpaCompliance, false );
				}
		);

		// MutableJpaComplianceImpl defaults its values based on the passed
		// `jpaByDefault` (`true` here).  ultimately we want to source this
		// from `AvailableSettings#JPA_COMPLIANCE`
		final MutableJpaComplianceImpl compliance = new MutableJpaComplianceImpl( Collections.emptyMap(), true );
		assertAll( compliance, true );
	}


}
