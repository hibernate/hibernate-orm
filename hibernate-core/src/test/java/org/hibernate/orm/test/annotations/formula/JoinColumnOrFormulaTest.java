/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import java.util.HashSet;
import java.util.Set;
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

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class JoinColumnOrFormulaTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey( value = "HHH-9897" )
	@FailureExpected( jiraKey = "HHH-9897" )
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

	@Entity( name = "A" )
	@Table( name = "A" )
	public static class A {
		@Id
		@Column( name = "idA")
		public Integer id;

		@OneToMany
		@JoinColumnOrFormula(formula = @JoinFormula(value = "idA", referencedColumnName = "idA"))
		Set<D> ds = new HashSet<D>();
	}

	@Entity( name = "D" )
	@Table( name = "D" )
	public static class D {
		@Id
		@Column( name = "idA")
		public Integer idA;
	}
}
