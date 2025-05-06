/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class UniqueConstraintUnitTests extends BaseUnitTestCase {

	@Test
	@JiraKey( value = "HHH-8026" )
	public void testUnNamedConstraints() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

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
					tableA.getUniqueKeys().values().iterator().next().getName().equals(
							tableB.getUniqueKeys().values().iterator().next().getName()
					)
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey( value = "HHH-8537" )
	public void testNonExistentColumn() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

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
