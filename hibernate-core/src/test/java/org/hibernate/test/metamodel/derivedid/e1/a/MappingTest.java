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
package org.hibernate.test.metamodel.derivedid.e1.a;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.test.metamodel.derivedid.e1.Employee;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class MappingTest extends BaseUnitTestCase {
	/**
	 * Test for the mapping produced from e1.a
	 */
	@Test
	public void testMapping() {
		MetadataSources sources = new MetadataSources()
				.addAnnotatedClass( Employee.class )
				.addAnnotatedClass( DependentId.class )
				.addAnnotatedClass( Dependent.class );
		Metadata metadata = sources.buildMetadata();

		EntityBinding employeeBinding = metadata.getEntityBinding( Employee.class.getName() );
		assertEquals(
				EntityIdentifierNature.SIMPLE,
				employeeBinding.getHierarchyDetails().getEntityIdentifier().getNature()
		);
		SingularAttributeBinding empIdAttrBinding = employeeBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding();
		Type empIdType = empIdAttrBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertNotNull( empIdType );
		assertTyping( LongType.class, empIdType );

		EntityBinding depBinding = metadata.getEntityBinding( Dependent.class.getName() );
		assertEquals(
				EntityIdentifierNature.NON_AGGREGATED_COMPOSITE,
				depBinding.getHierarchyDetails().getEntityIdentifier().getNature()
		);
		EntityIdentifier.NonAggregatedCompositeIdentifierBinding identifierBinding = assertTyping(
				EntityIdentifier.NonAggregatedCompositeIdentifierBinding.class,
				depBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding()
		);

		assertNotNull( identifierBinding.getIdClassMetadata() );
		assertNotNull( identifierBinding.getIdClassMetadata().getIdClassType() );
		assertNotNull( identifierBinding.getIdClassMetadata().getEmbeddableBinding() );

		// The issue here is the lack of understanding that the IdClass attributes
		// are not the same types as the entity attribute(s).
		//
		// For example, here we currently assume that the type of DependentId.emp
		// matches the Dependent.emp type
	}
}
