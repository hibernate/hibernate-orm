/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.namingstrategy.components;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class ComponentNamingStrategyTest extends BaseUnitTestCase {
	@Test
	public void testDefaultNamingStrategy() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( Container.class ).addAnnotatedClass( Item.class );

			final Metadata metadata = ms.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
					.build();

			final PersistentClass pc = metadata.getEntityBinding( Container.class.getName() );
			Property p = pc.getProperty( "items" );
			Bag value = assertTyping( Bag.class, p.getValue() );
			SimpleValue elementValue = assertTyping( SimpleValue.class, value.getElement() );
			assertEquals( 1, elementValue.getColumnSpan() );
			Column column = assertTyping( Column.class, elementValue.getColumnIterator().next() );
			assertEquals( column.getName(), "name" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6005" )
	public void testComponentSafeNamingStrategy() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( Container.class ).addAnnotatedClass( Item.class );

			final Metadata metadata = ms.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE )
					.build();

			final PersistentClass pc = metadata.getEntityBinding( Container.class.getName() );
			Property p = pc.getProperty( "items" );
			Bag value = assertTyping( Bag.class, p.getValue() );
			SimpleValue elementValue = assertTyping(  SimpleValue.class, value.getElement() );
			assertEquals( 1, elementValue.getColumnSpan() );
			Column column = assertTyping( Column.class, elementValue.getColumnIterator().next() );
			assertEquals( column.getName(), "items_name" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
