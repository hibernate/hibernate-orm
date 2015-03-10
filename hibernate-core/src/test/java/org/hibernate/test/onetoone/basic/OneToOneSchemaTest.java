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
package org.hibernate.test.onetoone.basic;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class OneToOneSchemaTest extends BaseUnitTestCase {

	@Test
	public void testUniqueKeyNotGeneratedViaAnnotations() throws Exception {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Parent.class )
					.addAnnotatedClass( Child.class )
					.buildMetadata();

			Table childTable = metadata.getDatabase().getDefaultSchema().locateTable( Identifier.toIdentifier( "CHILD" ) );
			assertFalse( "UniqueKey was generated when it should not", childTable.getUniqueKeyIterator().hasNext() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
