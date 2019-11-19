/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.Version;

import org.hibernate.annotations.Table;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

public class OptionalSecondaryTableBatchTest extends BaseNonConfigCoreFunctionalTestCase {
	private List<Company> companies;

	@Test
	public void testMerge() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						final Company company = companies.get( i );
						company.taxNumber = 2 * i;
						session.merge( company );
					}
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						assertEquals( Integer.valueOf( 2 * i ), session.get( Company.class, i).taxNumber );
					}
				}
		);
	}

	@Test
	public void testSaveOrUpdate() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						final Company company = companies.get( i );
						company.taxNumber = 2 * i;
						session.saveOrUpdate( company );
					}
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						assertEquals( Integer.valueOf( 2 * i ), session.get( Company.class, i).taxNumber );
					}
				}
		);
	}

	@Test
	public void testUpdate() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						final Company company = companies.get( i );
						company.taxNumber = 2 * i;
						session.update( company );
					}
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0 ; i < 10 ; i++ ) {
						assertEquals( Integer.valueOf( 2 * i ), session.get( Company.class, i).taxNumber );
					}
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Company.class };
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, 5 );
	}

	@Before
	public void setupData() {
		companies = new ArrayList<>( 10 );
		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0; i < 10; i++ ) {
						final Company company = new Company();
						company.id = i;
						if ( i % 2 == 0 ) {
							company.taxNumber = i;
						}
						session.persist( company );
						companies.add( company );
					}
				}
		);
	}

	@After
	public void cleanupData() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.createQuery( "delete from Company" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Company")
	@SecondaryTable( name = "company_tax" )
	@Table( appliesTo = "company_tax", optional = true)
	public static class Company {

		@Id
		private int id;

		@Version
		@Column( name = "ver" )
		private int version;

		private String name;

		@Column(table = "company_tax")
		private Integer taxNumber;
	}
}
