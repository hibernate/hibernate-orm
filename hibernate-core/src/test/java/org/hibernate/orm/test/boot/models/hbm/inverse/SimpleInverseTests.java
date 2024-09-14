/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.inverse;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleInverseTests {
	@Test
	@DomainModel( annotatedClasses = {Customer.class, Order.class} )
	void testAnnotations(DomainModelScope modelScope) {
		verify( modelScope );
	}

	@Test
	@DomainModel(xmlMappings = "mappings/models/hbm/inverse/mapping.xml")
	void testMappingXml(DomainModelScope modelScope) {
		verify( modelScope );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel(xmlMappings = "mappings/models/hbm/inverse/hbm.xml")
	void testHbmXml(DomainModelScope modelScope) {
		verify( modelScope );
	}

	private void verify(DomainModelScope modelScope) {
		{
			final PersistentClass customerEntityBinding = modelScope.getEntityBinding( Customer.class );
			final Property ordersProperty = customerEntityBinding.getProperty( "orders" );
			final Collection ordersCollection = (Collection) ordersProperty.getValue();
			final KeyValue ordersCollectionKey = ordersCollection.getKey();
			assertThat( ordersCollectionKey.getColumns() ).hasSize( 1 );
			assertThat( ordersCollectionKey.getColumns().get( 0 ).getName() ).isEqualTo( "customer_fk" );
			final OneToMany childrenPropertyElement = (OneToMany) ordersCollection.getElement();
			assertThat( ordersCollection.isInverse() ).isTrue();
			assertThat( childrenPropertyElement.getColumns() ).hasSize( 1 );
			assertThat( childrenPropertyElement.getColumns().get( 0 ).getName() ).isEqualTo( "id" );
		}

		{
			final PersistentClass orderEntityBinding = modelScope.getEntityBinding( Order.class );
			final Property customerProperty = orderEntityBinding.getProperty( "customer" );
			final ToOne customerPropertyValue = (ToOne) customerProperty.getValue();
			assertThat( customerPropertyValue.isReferenceToPrimaryKey() ).isTrue();
			assertThat( customerPropertyValue.getColumns() ).hasSize( 1 );
			assertThat( customerPropertyValue.getColumns().get( 0 ).getName() ).isEqualTo( "customer_fk" );
		}
	}
}
