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
package org.hibernate.test.annotations.uniqueconstraint;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class UniqueConstraintUnitTests extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-8026" )
	public void testUnNamedConstraints() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( UniqueNoNameA.class )
					.addAnnotatedClass( UniqueNoNameB.class )
					.buildMetadata();

			org.hibernate.mapping.Table tableA = null;
			org.hibernate.mapping.Table tableB = null;
			for ( org.hibernate.mapping.Table table : metadata.collectTableMappings() ) {
				if ( table.getName().equals( "UniqueNoNameA" ) ) {
					tableA = table;
				}
				else if ( table.getName().equals( "UniqueNoNameB" ) ) {
					tableB = table;
				}
			}

			assertTrue( "Could not find the expected tables.", tableA != null && tableB != null );
			assertFalse(
					tableA.getUniqueKeyIterator().next().getName().equals(
							tableB.getUniqueKeyIterator().next().getName()
					)
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8537" )
	public void testNonExistentColumn() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( UniqueNoNameA.class )
					.addAnnotatedClass( UniqueNoNameB.class )
					.buildMetadata();
		}
		catch (NullPointerException e) {
			fail( "The @UniqueConstraint with a non-existent column name should have resulted in an AnnotationException" );
		}
		catch (AnnotationException e) {
			// expected
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	@Table( name = "UniqueNoNameA",
			uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
	public static class UniqueNoNameA {
		@Id
		@GeneratedValue
		public long id;

		public String name;
	}

	@Entity
	@Table( name = "UniqueNoNameB",
			uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
	public static class UniqueNoNameB {
		@Id
		@GeneratedValue
		public long id;

		public String name;
	}

	@Entity
	public static class UniqueColumnDoesNotExist {
		@Id
		public Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable(
				name = "tbl_strings",
				joinColumns = @JoinColumn(name = "fk", nullable = false),
				// the failure required at least 1 columnName to be correct -- all incorrect wouldn't reproduce
				uniqueConstraints =  @UniqueConstraint(columnNames = { "fk", "doesnotexist" })
		)
		@Column(name = "string", nullable = false)
		public Set<String> strings = new HashSet<String>();
	}
}
