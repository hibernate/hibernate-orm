/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.id.Assigned;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@RequiresDialect(H2Dialect.class)
public class IdentifierGeneratorTest extends BaseAnnotationBindingTestCase {
	@Entity
	class NoGenerationEntity {
		@Id
		private long id;
	}

	@Test
	@Resources(annotatedClasses = NoGenerationEntity.class)
	public void testNoIdGeneration() {
		EntityBinding binding = getEntityBinding( NoGenerationEntity.class );
        EntityIdentifier identifier = binding.getHierarchyDetails().getEntityIdentifier();
		IdentifierGenerator generator =identifier.getIdentifierGenerator();
        assertNotNull( generator );
        assertEquals( "Wrong generator", Assigned.class, generator.getClass() );
        assertEquals( EntityIdentifierNature.SIMPLE, identifier.getNature() );
	}

	@Entity
	class AutoEntity {
		@Id
		@GeneratedValue
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = AutoEntity.class)
	public void testAutoGenerationType() {
		EntityBinding binding = getEntityBinding( AutoEntity.class );
		IdentifierGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator();

		assertEquals( "Wrong generator", IdentityGenerator.class, generator.getClass() );
	}

	@Entity
	class TableEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = TableEntity.class)
	public void testTableGenerationType() {
		EntityBinding binding = getEntityBinding( TableEntity.class );
		IdentifierGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator();

		assertEquals( "Wrong generator", MultipleHiLoPerTableGenerator.class, generator.getClass() );
	}

	@Entity
	class SequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = SequenceEntity.class)
	public void testSequenceGenerationType() {
		EntityBinding binding = getEntityBinding( SequenceEntity.class );
		IdentifierGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator();

		assertEquals( "Wrong generator", SequenceHiLoGenerator.class, generator.getClass() );
	}


	@Entity
	class NamedGeneratorEntity {
		@Id
		@GeneratedValue(generator = "my-generator")
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	public void testUndefinedGenerator() {
		try {
			sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
			sources.addAnnotatedClass( NamedGeneratorEntity.class );
			sources.buildMetadata();
			fail();
		}
		catch ( MappingException e ) {
			assertTrue( e.getMessage().contains( "Unable to find named generator" ) );
		}
	}

	@Entity
	@GenericGenerator(name = "my-generator", strategy = "uuid")
	class NamedGeneratorEntity2 {
		@Id
		@GeneratedValue(generator = "my-generator")
		private long id;

		public long getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = NamedGeneratorEntity2.class)
	public void testNamedGenerator() {
		EntityBinding binding = getEntityBinding( NamedGeneratorEntity2.class );
		IdentifierGenerator generator = binding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator();

		assertEquals( "Wrong generator", UUIDHexGenerator.class, generator.getClass() );
	}
}


