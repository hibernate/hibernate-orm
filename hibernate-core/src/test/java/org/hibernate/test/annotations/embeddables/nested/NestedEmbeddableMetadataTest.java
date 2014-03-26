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
package org.hibernate.test.annotations.embeddables.nested;

import java.sql.Types;

import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingEmbedded;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.type.CustomType;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertJdbcTypeCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class NestedEmbeddableMetadataTest extends BaseUnitTestCase {
	@Test
	@FailureExpectedWithNewMetamodel
	public void testEnumTypeInterpretation() {
		Metadata metadata = new MetadataSources().addAnnotatedClass( Customer.class ).buildMetadata();
		EntityBinding eb = metadata.getEntityBinding( Customer.class.getName() );
		PluralAttributeBinding investmentsBinding = (PluralAttributeBinding) eb.locateAttributeBinding( "investments" );
		PluralAttributeElementBindingEmbedded investmentsElementBinding = (PluralAttributeElementBindingEmbedded) investmentsBinding.getPluralAttributeElementBinding();
		EmbeddedAttributeBinding amountBinding = (EmbeddedAttributeBinding) investmentsElementBinding.getEmbeddableBinding().locateAttributeBinding(
				"amount"
		);
		SingularAttributeBinding currencyBinding = (SingularAttributeBinding) amountBinding.getEmbeddableBinding().locateAttributeBinding(
				"currency"
		);

		CustomType currencyType = (CustomType) currencyBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		assertNotNull( currencyType );
		int[] currencySqlTypes = currencyType.sqlTypes( null );
		assertEquals( 1, currencySqlTypes.length );
		assertJdbcTypeCode( Types.VARCHAR, currencySqlTypes[0] );
	}
}
