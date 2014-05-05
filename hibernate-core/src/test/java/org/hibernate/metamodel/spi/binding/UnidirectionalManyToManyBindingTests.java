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
package org.hibernate.metamodel.spi.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gail Badner
 */
@FailureExpectedWithNewUnifiedXsd(message = "extra lazy not yet supported in the unified schema")
public class UnidirectionalManyToManyBindingTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7436" )
	public void testHbm() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		sources.addResource( "org/hibernate/metamodel/spi/binding/EntityWithUnidirectionalManyToManys.hbm.xml" );
		sources.addResource( "org/hibernate/metamodel/spi/binding/SimpleEntity.hbm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.getMetadataBuilder().build();

		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithUnidirectionalManyToMany.class.getName() );
		final EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertNotNull( entityBinding );

		assertEquals(
				Identifier.toIdentifier( "SimpleEntity" ),
				simpleEntityBinding.getPrimaryTable().getLogicalName()
		);
		assertEquals( 1, simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() );
		Column simpleEntityIdColumn = simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 );
		assertEquals( Identifier.toIdentifier( "id" ) , simpleEntityIdColumn.getColumnName() );
	}
}
