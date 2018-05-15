/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables.nested.fieldaccess;

import java.sql.Types;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.CustomType;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertJdbcTypeCode;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class FieldAccessedNestedEmbeddableMetadataTest extends BaseUnitTestCase {
	@Test
	@FailureExpected( jiraKey = "HHH-9089" )
	public void testEnumTypeInterpretation() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Customer.class )
					.buildMetadata();

			PersistentClass classMetadata = metadata.getEntityBinding( Customer.class.getName() );
			Property investmentsProperty = classMetadata.getProperty( "investments" );
			Collection investmentsValue = (Collection) investmentsProperty.getValue();
			Component investmentMetadata = (Component) investmentsValue.getElement();
			Value descriptionValue = investmentMetadata.getProperty( "description" ).getValue();
			assertEquals( 1, descriptionValue.getColumnSpan() );
			Column selectable = (Column) descriptionValue.getColumnIterator().next();
			assertEquals( 500, selectable.getLength() );
			Component amountMetadata = (Component) investmentMetadata.getProperty( "amount" ).getValue();
			SimpleValue currencyMetadata = (SimpleValue) amountMetadata.getProperty( "currency" ).getValue();
			CustomType currencyType = (CustomType) currencyMetadata.getType();
			int[] currencySqlTypes = currencyType.sqlTypes( metadata );
			assertEquals( 1, currencySqlTypes.length );
			assertJdbcTypeCode( Types.VARCHAR, currencySqlTypes[0] );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
