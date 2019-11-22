/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.DomainMetamodel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SimpleEntity.class,
				EntityContainingLists.class,
				EntityContainingSets.class,
				EntityContainingMaps.class,
				SomeStuff.class
		}
)
@ServiceRegistry
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class PluralAttributeMappingTests {

	@Test
	public void testLists(SessionFactoryScope scope) {
		final DomainMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityContainingLists.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 6 ) );

		final AttributeMapping listOfBasics = containerEntityDescriptor.findAttributeMapping( "listOfBasics" );
		assertThat( listOfBasics, notNullValue() );

		final AttributeMapping listOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "listOfConvertedBasics" );
		assertThat( listOfConvertedBasics, notNullValue() );


		final AttributeMapping listOfEnums = containerEntityDescriptor.findAttributeMapping( "listOfEnums" );
		assertThat( listOfEnums, notNullValue() );

		final AttributeMapping listOfComponents = containerEntityDescriptor.findAttributeMapping( "listOfComponents" );
		assertThat( listOfComponents, notNullValue() );

		final AttributeMapping listOfEntities = containerEntityDescriptor.findAttributeMapping( "listOfEntities" );
		assertThat( listOfEntities, notNullValue() );
	}

	@Test
	public void testSets(SessionFactoryScope scope) {
		final DomainMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityContainingSets.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 6 ) );

		final AttributeMapping setOfBasics = containerEntityDescriptor.findAttributeMapping( "setOfBasics" );
		assertThat( setOfBasics, notNullValue() );

		final AttributeMapping setOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "setOfConvertedBasics" );
		assertThat( setOfConvertedBasics, notNullValue() );


		final AttributeMapping setOfEnums = containerEntityDescriptor.findAttributeMapping( "setOfEnums" );
		assertThat( setOfEnums, notNullValue() );

		final AttributeMapping setOfComponents = containerEntityDescriptor.findAttributeMapping( "setOfComponents" );
		assertThat( setOfComponents, notNullValue() );

		final AttributeMapping setOfEntities = containerEntityDescriptor.findAttributeMapping( "setOfEntities" );
		assertThat( setOfEntities, notNullValue() );
	}

	@Test
	public void testMaps(SessionFactoryScope scope) {
		final DomainMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityContainingMaps.class );

		// 8 for now, until entity-valued map keys is supported
		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 9 ) );

		final PluralAttributeMapping basicByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByBasic" );
		assertThat( basicByBasic, notNullValue() );
		assertThat( basicByBasic.getKeyDescriptor(), notNullValue() );
		assertThat( basicByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByEnum = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByEnum" );
		assertThat( basicByEnum, notNullValue() );
		assertThat( basicByEnum.getKeyDescriptor(), notNullValue() );
		assertThat( basicByEnum.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByConvertedEnum = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByConvertedEnum" );
		assertThat( basicByConvertedEnum, notNullValue() );
		assertThat( basicByConvertedEnum.getKeyDescriptor(), notNullValue() );
		assertThat( basicByConvertedEnum.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping someStuffByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "someStuffByBasic" );
		assertThat( someStuffByBasic, notNullValue() );
		assertThat( someStuffByBasic.getKeyDescriptor(), notNullValue() );
		assertThat( someStuffByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicBySomeStuff = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicBySomeStuff" );
		assertThat( basicBySomeStuff, notNullValue() );
		assertThat( basicBySomeStuff.getKeyDescriptor(), notNullValue() );
		assertThat( basicBySomeStuff.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping oneToManyByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "oneToManyByBasic" );
		assertThat( oneToManyByBasic, notNullValue() );
		assertThat( oneToManyByBasic.getKeyDescriptor(), notNullValue() );
		assertThat( oneToManyByBasic.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping basicByOneToMany = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "basicByOneToMany" );
		assertThat( basicByOneToMany, notNullValue() );
		assertThat( basicByOneToMany.getKeyDescriptor(), notNullValue() );
		assertThat( basicByOneToMany.getElementDescriptor(), notNullValue() );

		final PluralAttributeMapping manyToManyByBasic = (PluralAttributeMapping) containerEntityDescriptor.findAttributeMapping( "manyToManyByBasic" );
		assertThat( manyToManyByBasic, notNullValue() );
		assertThat( manyToManyByBasic.getKeyDescriptor(), notNullValue() );
		assertThat( manyToManyByBasic.getElementDescriptor(), notNullValue() );
	}

}
