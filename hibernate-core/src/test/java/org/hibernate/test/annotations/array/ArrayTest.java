/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.array;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.test.annotations.array.Contest.Month;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOneToMany() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Competitor c1 = new Competitor();
		c1.setName( "Renault" );
		Competitor c2 = new Competitor();
		c2.setName( "Ferrari" );
		Contest contest = new Contest();
		contest.setResults( new Competitor[]{c1, c2} );
		contest.setHeldIn(new Month[]{Month.January, Month.December});
		s.persist( contest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		contest = (Contest) s.get( Contest.class, contest.getId() );
		assertNotNull( contest );
		assertNotNull( contest.getResults() );
		assertEquals( 2, contest.getResults().length );
		assertEquals( c2.getName(), contest.getResults()[1].getName() );
		assertEquals( 2, contest.getHeldIn().length );
		assertEquals( Month.January, contest.getHeldIn()[0] );
		tx.commit();
		s.close();
	}
	
	@Test
	public void testNoIndexAnnotationFailure() {
		Properties properties = constructProperties();
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		StandardServiceRegistry serviceRegistry = buildServiceRegistry( bootRegistry, properties );
		MetadataSources sources = new MetadataSources( bootRegistry );
		sources.addAnnotatedClass( NoIndexArrayEntity.class );
		MetadataBuilder metadataBuilder = sources.getMetadataBuilder(serviceRegistry);
		boolean caught = false;
		try {
			metadataBuilder.build();
		}
		catch ( AnnotationException e ) {
			caught = true;
			assertTrue( e.getMessage().contains( "must be annotated with @OrderColumn or @IndexColumn" ) );
		}
		assertTrue( caught );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Competitor.class, Contest.class, Contest.Month.class };
	}
	
	@Entity
	public static class NoIndexArrayEntity {
		@Id
		@GeneratedValue
		public long id;
		
		@ElementCollection
		public NoIndexArrayEntity[] subElements;
	}
}
