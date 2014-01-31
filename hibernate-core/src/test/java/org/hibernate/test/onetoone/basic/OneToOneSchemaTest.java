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

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class OneToOneSchemaTest extends BaseUnitTestCase {

	@Test
	public void testUniqueKeyNotGeneratedViaAnnotations() throws Exception {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources()
				.addAnnotatedClass( Parent.class )
				.addAnnotatedClass( Child.class )
				.buildMetadata();

		probeForUniqueKey( metadata );
	}

	private void probeForUniqueKey(MetadataImplementor metadata) {
		org.hibernate.metamodel.spi.relational.TableSpecification table = SchemaUtil.getTable( "CHILD", metadata );
		assertFalse( "UniqueKey was generated when it should not", table.getUniqueKeys().iterator().hasNext() );
	}
}
