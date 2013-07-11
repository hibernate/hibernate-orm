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

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.CustomType;

import static org.hibernate.testing.junit4.ExtraAssertions.assertJdbcTypeCode;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class NestedEmbeddableMetadataTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, Investment.class, MonetaryAmount.class };
	}

	@Test
	public void testEnumTypeInterpretation() {

		CustomType currencyType = getCurrencyAttributeType();
		int[] currencySqlTypes = currencyType.sqlTypes( sessionFactory() );
		assertEquals( 1, currencySqlTypes.length );
		assertJdbcTypeCode( Types.VARCHAR, currencySqlTypes[0] );

	}

	private CustomType getCurrencyAttributeType() {
		if ( isMetadataUsed() ) {
			return getCustomTypeFromMetamodel();
		}
		else {
			return getCustomTypeFromConfiguration();
		}
	}

	private CustomType getCustomTypeFromMetamodel() {
		EntityBinding entityBinding = getEntityBinding( Customer.class );
		PluralAttributeBinding attributeBinding = (PluralAttributeBinding) entityBinding.locateAttributeBinding(
				"investments"
		);
		CompositePluralAttributeElementBinding pluralAttributeElementBinding = (CompositePluralAttributeElementBinding) attributeBinding
				.getPluralAttributeElementBinding();

		CompositeAttributeBinding compositeAttributeBinding = (CompositeAttributeBinding) pluralAttributeElementBinding
				.getCompositeAttributeBindingContainer()
				.locateAttributeBinding( "amount" );

		SingularAttributeBinding currencyAttributeBinding = (SingularAttributeBinding) compositeAttributeBinding.locateAttributeBinding(
				"currency"
		);
		return (CustomType) currencyAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
	}

	private CustomType getCustomTypeFromConfiguration() {
		PersistentClass classMetadata = configuration().getClassMapping( Customer.class.getName() );
		Property investmentsProperty = classMetadata.getProperty( "investments" );
		Collection investmentsValue = (Collection) investmentsProperty.getValue();
		Component investmentMetadata = (Component) investmentsValue.getElement();
		Component amountMetadata = (Component) investmentMetadata.getProperty( "amount" ).getValue();
		SimpleValue currencyMetadata = (SimpleValue) amountMetadata.getProperty( "currency" ).getValue();
		return (CustomType) currencyMetadata.getType();
	}
}
