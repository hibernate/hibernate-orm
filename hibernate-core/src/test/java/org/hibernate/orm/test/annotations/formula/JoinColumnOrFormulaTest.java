/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class JoinColumnOrFormulaTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void before() {
		final StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		if ( !Environment.getProperties().containsKey( AvailableSettings.CONNECTION_PROVIDER )
			&& !builder.getSettings().containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			builder.applySetting(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProvider.getInstance()
			);
			builder.applySetting(
					AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT,
					Boolean.TRUE
			);
		}

		ssr = builder.build();
	}

	@AfterEach
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-9897")
	@FailureExpected(jiraKey = "HHH-9897")
	public void testUseOfJoinColumnOrFormula() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( D.class )
				.buildMetadata();

		// Binding to the mapping model works after the simple change for HHH-9897
		// But building the SessionFactory fails in the collection persister trying to
		// use the formula (it expects Columns too)
		metadata.buildSessionFactory().close();
	}

	@Entity(name = "A")
	@Table(name = "A")
	public static class A {
		@Id
		@Column(name = "idA")
		public Integer id;

		@OneToMany
		@JoinColumnOrFormula(formula = @JoinFormula(value = "idA", referencedColumnName = "idA"))
		Set<D> ds = new HashSet<>();
	}

	@Entity(name = "D")
	@Table(name = "D")
	public static class D {
		@Id
		@Column(name = "idA")
		public Integer idA;
	}
}
