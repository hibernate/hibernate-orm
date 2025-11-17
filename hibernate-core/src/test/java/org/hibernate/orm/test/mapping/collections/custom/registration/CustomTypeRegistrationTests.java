/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.registration;

import org.hibernate.collection.internal.CustomCollectionTypeSemantics;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.mapping.collections.custom.basic.MyListType;
import org.hibernate.type.CustomCollectionType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.metamodel.CollectionClassification.LIST;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { User.class, Email.class } )
public class CustomTypeRegistrationTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( User.class, (userDescriptor) -> {
			final Property emailAddressesProperty = userDescriptor.getProperty( "emailAddresses" );
			final Collection emailAddressesMapping = (Collection) emailAddressesProperty.getValue();
			assertThat( emailAddressesMapping.getCollectionSemantics().getCollectionClassification() ).isEqualTo( LIST );
			assertThat( emailAddressesMapping.getCollectionSemantics() ).isInstanceOf( CustomCollectionTypeSemantics.class );
			final CustomCollectionTypeSemantics semantics = (CustomCollectionTypeSemantics) emailAddressesMapping.getCollectionSemantics();
			assertThat( semantics.getCollectionType() ).isInstanceOf( CustomCollectionType.class );
			final CustomCollectionType collectionType = (CustomCollectionType) semantics.getCollectionType();
			assertThat( collectionType.getUserType() ).isInstanceOf( MyListType.class );
		} );
	}
}
