/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.basics;

import java.sql.Types;
import java.util.List;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.BasicType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BagIdTypeResolutionTests.EntityWithBag.class )
public class BagIdTypeResolutionTests {
	@Test
	public void testBagIdResolution(DomainModelScope scope) {
		final PersistentClass entityDescriptor = scope.getDomainModel().getEntityBinding( EntityWithBag.class.getName() );
		final Property namesDescriptor = entityDescriptor.getProperty( "names" );
		final IdentifierBag namesTypeDescriptor = (IdentifierBag) namesDescriptor.getValue();
		final BasicValue identifier = (BasicValue) namesTypeDescriptor.getIdentifier();
		final BasicValue.Resolution<?> identifierResolution = identifier.resolve();

		final BasicType<?> legacyResolvedBasicType = identifierResolution.getLegacyResolvedBasicType();
		assertThat( legacyResolvedBasicType.getJdbcType().getJdbcTypeCode(), equalTo( Types.SMALLINT ) );
//		assertThat( identifier.getIdentifierGeneratorStrategy(), equalTo( "increment" ) );
	}

	@Entity( name = "EntityWithBag" )
	@Table( name = "entity_with_bag" )
	public static class EntityWithBag {
		@Id
		private Integer id;
		private String name;
		@ElementCollection
		@CollectionId( column = @Column( name = "bag_id" ), generator = "increment" )
		@CollectionIdJdbcTypeCode( Types.SMALLINT )
		private List<String> names;
	}
}
