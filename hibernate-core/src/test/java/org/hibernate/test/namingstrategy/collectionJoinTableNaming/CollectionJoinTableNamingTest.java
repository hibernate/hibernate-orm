/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.namingstrategy.collectionJoinTableNaming;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 * @author Alessandro Polverini
 */
public class CollectionJoinTableNamingTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingLegacyStrategy() {
		Configuration cfg = new Configuration();
		cfg.setNamingStrategy( ImprovedNamingStrategy.INSTANCE );

		cfg.addAnnotatedClass( Input.class );
		cfg.addAnnotatedClass( Ptx.class );
		cfg.buildMappings();

		Collection inputs1Mapping = cfg.getCollectionMapping( Ptx.class.getName() + ".inputs1" );
		assertEquals( "ptx_inputs1", inputs1Mapping.getCollectionTable().getName() );

		Collection inputs2Mapping = cfg.getCollectionMapping( Ptx.class.getName() + ".inputs2" );
		assertEquals( "ptx_inputs2", inputs2Mapping.getCollectionTable().getName() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingImprovedDelegatorStrategy() {
		Configuration cfg = new Configuration();
		cfg.setNamingStrategyDelegator( ImprovedNamingStrategyDelegator.DEFAULT_INSTANCE );

		cfg.addAnnotatedClass( Input.class );
		cfg.addAnnotatedClass( Ptx.class );
		cfg.buildMappings();

		assertSameTableUsed( cfg );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingBase() {
		// really the same test as #testCollectionJoinTableNamingJpaCompliantStrategy,
		// here we just pick up the (same) NamingStrategy by default
		Configuration cfg = new Configuration();

		cfg.addAnnotatedClass( Input.class );
		cfg.addAnnotatedClass( Ptx.class );
		cfg.buildMappings();

		assertSameTableUsed( cfg );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingJpaCompliantStrategy() {
		// Even in 4.3, with JPA compliant naming, Hibernate creates an unusable table...

		Configuration cfg = new Configuration();
		cfg.setNamingStrategy( EJB3NamingStrategy.INSTANCE );

		cfg.addAnnotatedClass( Input.class );
		cfg.addAnnotatedClass( Ptx.class );
		cfg.buildMappings();

		assertSameTableUsed( cfg );
	}

	protected void assertSameTableUsed(Configuration cfg) {
		Collection inputs1Mapping = cfg.getCollectionMapping( Ptx.class.getName() + ".inputs1" );
		assertEquals( "ptx_input", inputs1Mapping.getCollectionTable().getName() );

		Collection inputs2Mapping = cfg.getCollectionMapping( Ptx.class.getName() + ".inputs2" );
		assertEquals( "ptx_input", inputs2Mapping.getCollectionTable().getName() );

		assertSame( inputs1Mapping.getCollectionTable(), inputs2Mapping.getCollectionTable() );

		// NOTE : here so that tester can more easily see the produced table. It is only dumped to stdout
		new SchemaExport( cfg ).create( true, false );

		for ( int i = 0; i < inputs1Mapping.getCollectionTable().getColumnSpan(); i++ ) {
			final Column column = inputs1Mapping.getCollectionTable().getColumn( i );

			// this, coupled with JPA saying the 2 collections implicitly map to the same table,
			// is the crux of the problem: all columns are null, so we effectively can never
			// insert rows into it.
			assertFalse( column.isNullable() );
		}
	}

	@Entity
	@Table(name = "ptx")
	public static class Ptx {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs1;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs2;

	}

	@Entity
	@Table(name = "input")
	public class Input implements Serializable {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;
	}
}
