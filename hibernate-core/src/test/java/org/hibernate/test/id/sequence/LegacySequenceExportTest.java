/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.sequence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedSequence;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class LegacySequenceExportTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void prepare() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" )
				.build();
	}

	@After
	public void destroy() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9936" )
	public void testMultipleUsesOfDefaultSequenceName() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Entity1.class )
				.addAnnotatedClass( Entity2.class )
				.buildMetadata();
		metadata.validate();

		assertEquals( 0, metadata.getDatabase().getAuxiliaryDatabaseObjects().size() );

		int count = 0;
		for ( MappedNamespace namespace : metadata.getDatabase().getNamespaces() ) {
			for ( MappedSequence sequence : namespace.getSequences() ) {
				count++;
			}
		}

		assertEquals( 1, count );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9936" )
	public void testMultipleUsesOfExplicitSequenceName() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Entity3.class )
				.addAnnotatedClass( Entity4.class )
				.buildMetadata();
		metadata.validate();

		assertEquals( 0, metadata.getDatabase().getAuxiliaryDatabaseObjects().size() );

		int count = 0;
		for ( MappedNamespace namespace : metadata.getDatabase().getNamespaces() ) {
			for ( MappedSequence sequence : namespace.getSequences() ) {
				count++;
			}
		}

		assertEquals( 1, count );
	}

	@Entity( name = "Entity1" )
	@Table( name = "Entity1" )
	public static class Entity1 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		public Integer id;
	}

	@Entity( name = "Entity2" )
	@Table( name = "Entity2" )
	public static class Entity2 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		public Integer id;
	}

	@Entity( name = "Entity3" )
	@Table( name = "Entity3" )
	public static class Entity3 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		@SequenceGenerator( name = "my_sequence" )
		public Integer id;
	}

	@Entity( name = "Entity4" )
	@Table( name = "Entity4" )
	public static class Entity4 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		@SequenceGenerator( name = "my_sequence" )
		public Integer id;
	}
}
