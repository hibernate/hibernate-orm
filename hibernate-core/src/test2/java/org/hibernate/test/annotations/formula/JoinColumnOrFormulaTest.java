/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.formula;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
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
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9897" )
	@FailureExpected( jiraKey = "HHH-9897" )
	public void testUseOfJoinColumnOrFormula() {
		Metadata metadata = new MetadataSources()
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
