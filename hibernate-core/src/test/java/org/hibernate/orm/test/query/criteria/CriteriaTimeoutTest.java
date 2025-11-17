/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.SpecHints;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Marco Belladelli
 */
@Jpa(
		annotatedClasses = CriteriaTimeoutTest.AnEntity.class,
		integrationSettings = @Setting(name = SpecHints.HINT_SPEC_QUERY_TIMEOUT, value = "123000"),
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		}
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@JiraKey("HHH-16062")
public class CriteriaTimeoutTest {
	private PreparedStatementSpyConnectionProvider connectionProvider;

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		final Map<String, Object> props = scope.getEntityManagerFactory().getProperties();
		connectionProvider = (PreparedStatementSpyConnectionProvider) props.get( AvailableSettings.CONNECTION_PROVIDER );
	}

	@BeforeEach
	public void clear() {
		connectionProvider.clear();
	}

	@Test
	public void testCreateQueryCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaQuery<AnEntity> criteriaSelect = entityManager.getCriteriaBuilder()
					.createQuery( AnEntity.class );
			criteriaSelect.select( criteriaSelect.from( AnEntity.class ) );
			entityManager.createQuery( criteriaSelect ).getResultList();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	@Test
	public void testCreateQueryCriteriaUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaUpdate<AnEntity> criteriaUpdate = entityManager.getCriteriaBuilder()
					.createCriteriaUpdate( AnEntity.class );
			criteriaUpdate.set( "name", "abc" );
			entityManager.createQuery( criteriaUpdate ).executeUpdate();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	@Test
	public void testCreateQueryCriteriaDelete(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaDelete<AnEntity> criteriaDelete = entityManager.getCriteriaBuilder()
					.createCriteriaDelete( AnEntity.class );
			entityManager.createQuery( criteriaDelete ).executeUpdate();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	@Test
	public void testCreateMutationQueryCriteriaUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaUpdate<AnEntity> criteriaUpdate = entityManager.getCriteriaBuilder()
					.createCriteriaUpdate( AnEntity.class );
			criteriaUpdate.set( "name", "abc" );
			final Session session = entityManager.unwrap( Session.class );
			session.createMutationQuery( criteriaUpdate ).executeUpdate();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	@Test
	public void testCreateMutationQueryCriteriaDelete(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaDelete<AnEntity> criteriaDelete = entityManager.getCriteriaBuilder()
					.createCriteriaDelete( AnEntity.class );
			final Session session = entityManager.unwrap( Session.class );
			session.createMutationQuery( criteriaDelete ).executeUpdate();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	@Test
	public void testCreateMutationQueryCriteriaInsertSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			final SqmCriteriaNodeBuilder criteriaBuilder = (SqmCriteriaNodeBuilder) session.getCriteriaBuilder();
			final SqmInsertSelectStatement<AnEntity> insertSelect = criteriaBuilder
					.createCriteriaInsertSelect( AnEntity.class );
			final SqmSelectStatement<Tuple> select = criteriaBuilder.createQuery( Tuple.class );
			select.multiselect( select.from( AnEntity.class ).get( "name" ) );
			insertSelect.addInsertTargetStateField( insertSelect.getTarget().get( "name" ) );
			insertSelect.setSelectQueryPart( select.getQuerySpec() );
			session.createMutationQuery( insertSelect ).executeUpdate();
			verifyQuerySetTimeoutWasCalled();
		} );
	}

	private void verifyQuerySetTimeoutWasCalled() {
		try {
			List<Object[]> setQueryTimeoutCalls = connectionProvider.spyContext.getCalls(
					Statement.class.getMethod( "setQueryTimeout", int.class ),
					connectionProvider.getPreparedStatements().get( 0 )
			);
			assertEquals( 2, setQueryTimeoutCalls.size() );
			assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
			assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
		}
		catch (Exception ex) {
			Assert.fail( "should not have thrown exception" );
		}
	}

	@Entity(name = "AnEntity")
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
