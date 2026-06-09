/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.List;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.bytecode.enhancement.cascade.CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest;
import org.hibernate.orm.test.bytecode.enhancement.lazy.LazyLoadingByEnhancerSetterTest;
import org.hibernate.orm.test.bytecode.enhancement.merge.MergeDetachedCascadedCollectionInEmbeddableTest;
import org.hibernate.orm.test.cascade.circle.Node;
import org.hibernate.orm.test.cascade.circle.Route;
import org.hibernate.orm.test.cascade.circle.Tour;
import org.hibernate.orm.test.cascade.circle.Transport;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class CascadeCircleXmlBindingTests {
	@Test
	void checkInverseOneToManyHandoff(ServiceRegistryScope registryScope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass nodeBinding = context.getMetadataCollector().getEntityBinding( Node.class.getName() );
					final PersistentClass transportBinding = context.getMetadataCollector().getEntityBinding( Transport.class.getName() );

					assertThat( transportBinding.getProperty( "pickupNode" ).getCascade() )
							.contains( "merge" )
							.contains( "persist" )
							.contains( "refresh" );
					assertThat( transportBinding.getProperty( "deliveryNode" ).getCascade() )
							.contains( "merge" )
							.contains( "persist" )
							.contains( "refresh" );
					assertThat( nodeBinding.getProperty( "pickupTransports" ).getCascade() )
							.contains( "merge" )
							.contains( "persist" )
							.contains( "refresh" );
					assertThat( nodeBinding.getProperty( "deliveryTransports" ).getCascade() )
							.contains( "merge" )
							.contains( "persist" )
							.contains( "refresh" );

					final Collection pickupTransports = (Collection) nodeBinding.getProperty( "pickupTransports" ).getValue();
					assertThat( pickupTransports.isInverse() ).isTrue();
					assertThat( pickupTransports.getMappedByProperty() ).isEqualTo( "pickupNode" );
					assertThat( pickupTransports.getCollectionTable().getName() ).isEqualTo( "HB_Transport" );
					assertThat( pickupTransports.getKey().getColumns().stream().map( Column::getName ) )
							.containsExactly( "pickupNodeID" );

					final ManyToOne pickupNode = (ManyToOne) transportBinding.getProperty( "pickupNode" ).getValue();
					assertThat( pickupNode.getReferencedEntityName() ).isEqualTo( Node.class.getName() );
					assertThat( pickupNode.isLazy() ).isFalse();
					assertThat( pickupNode.getColumns().stream().map( Column::getName ) )
							.containsExactly( "pickupNodeID" );
					assertThat( pickupNode.isNullable() ).isFalse();
					assertThat( transportBinding.getProperty( "pickupNode" ).isOptional() ).isFalse();

					final Collection deliveryTransports = (Collection) nodeBinding.getProperty( "deliveryTransports" ).getValue();
					assertThat( deliveryTransports.isInverse() ).isTrue();
					assertThat( deliveryTransports.getMappedByProperty() ).isEqualTo( "deliveryNode" );
					assertThat( deliveryTransports.getCollectionTable().getName() ).isEqualTo( "HB_Transport" );
					assertThat( deliveryTransports.getKey().getColumns().stream().map( Column::getName ) )
							.containsExactly( "deliveryNodeID" );

					final ManyToOne deliveryNode = (ManyToOne) transportBinding.getProperty( "deliveryNode" ).getValue();
					assertThat( deliveryNode.getReferencedEntityName() ).isEqualTo( Node.class.getName() );
					assertThat( deliveryNode.isLazy() ).isFalse();
					assertThat( deliveryNode.getColumns().stream().map( Column::getName ) )
							.containsExactly( "deliveryNodeID" );
					assertThat( deliveryNode.isNullable() ).isFalse();
					assertThat( transportBinding.getProperty( "deliveryNode" ).isOptional() ).isFalse();
				},
				registryScope.getRegistry(),
				List.of( "org/hibernate/orm/test/cascade/circle/MultiPathCircleCascade.orm.xml" ),
				Route.class,
				Node.class,
				Tour.class,
				Transport.class
		);
	}

	@Test
	void checkLazyPluralAttributeHandoff(ServiceRegistryScope registryScope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass personBinding = context.getMetadataCollector()
							.getEntityBinding(
									CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest.Person.class.getName()
							);
					final var addressesProperty = personBinding.getProperty( "addresses" );
					final Collection addresses = (Collection) addressesProperty.getValue();

					assertThat( addresses.isLazy() ).isTrue();
					assertThat( addressesProperty.isLazy() ).isTrue();
				},
				registryScope.getRegistry(),
				CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest.Person.class,
				CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest.Address.class
		);
	}

	@Test
	void checkElementCollectionMapLobHandoff(ServiceRegistryScope registryScope) throws ClassNotFoundException {
		final Class<?> itemFieldClass = Class.forName(
				LazyLoadingByEnhancerSetterTest.class.getName() + "$ItemField"
		);
		final Class<?> itemPropertyClass = Class.forName(
				LazyLoadingByEnhancerSetterTest.class.getName() + "$ItemProperty"
		);

		checkDomainModel(
				(context) -> {
					final PersistentClass itemFieldBinding = context.getMetadataCollector()
							.getEntityBinding( itemFieldClass.getName() );
					final org.hibernate.mapping.Map parameters =
							(org.hibernate.mapping.Map) itemFieldBinding.getProperty( "parameters" ).getValue();
					final BasicValue key = (BasicValue) parameters.getIndex();
					final BasicValue element = (BasicValue) parameters.getElement();

					assertThat( key.getColumns().stream().map( Column::getName ) ).containsExactly( "NAME" );
					assertThat( key.isLob() ).isFalse();
					assertThat( element.getColumns().stream().map( Column::getName ) ).containsExactly( "PARAM_VAL" );
					assertThat( element.isLob() ).isTrue();
				},
				registryScope.getRegistry(),
				itemFieldClass,
				itemPropertyClass
		);
	}

	@Test
	void checkEmbeddablePluralAssociationTypeHandoff(ServiceRegistryScope registryScope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass headingBinding = context.getMetadataCollector().getEntityBinding(
							MergeDetachedCascadedCollectionInEmbeddableTest.Heading.class.getName()
					);
					final Component grouping = (Component) headingBinding.getProperty( "grouping" ).getValue();
					final Collection things = (Collection) grouping.getProperty( "things" ).getValue();
					final OneToMany element = (OneToMany) things.getElement();

					assertThat( element.getReferencedEntityName() )
							.isEqualTo( MergeDetachedCascadedCollectionInEmbeddableTest.Thing.class.getName() );
				},
				registryScope.getRegistry(),
				MergeDetachedCascadedCollectionInEmbeddableTest.Heading.class,
				MergeDetachedCascadedCollectionInEmbeddableTest.Grouping.class,
				MergeDetachedCascadedCollectionInEmbeddableTest.Thing.class
		);
	}

	@Test
	void checkNestedEmbeddableElementCollectionTypeHandoff(ServiceRegistryScope registryScope) throws ClassNotFoundException {
		final Class<?> parentEntityClass = Class.forName(
				"org.hibernate.orm.test.bytecode.enhancement.merge.CompositeMergeTest$ParentEntity"
		);
		final Class<?> addressClass = Class.forName(
				"org.hibernate.orm.test.bytecode.enhancement.merge.CompositeMergeTest$Address"
		);
		final Class<?> countryClass = Class.forName(
				"org.hibernate.orm.test.bytecode.enhancement.merge.CompositeMergeTest$Country"
		);

		checkDomainModel(
				(context) -> {
					final PersistentClass parentBinding = context.getMetadataCollector()
							.getEntityBinding( parentEntityClass.getName() );
					final Component address = (Component) parentBinding.getProperty( "address" ).getValue();
					final Component country = (Component) address.getProperty( "country" ).getValue();
					final Collection languages = (Collection) country.getProperty( "languages" ).getValue();
					final BasicValue element = (BasicValue) languages.getElement();

					assertThat( element.getResolution().getDomainJavaType().getJavaTypeClass() ).isEqualTo( String.class );
				},
				registryScope.getRegistry(),
				parentEntityClass,
				addressClass,
				countryClass
		);
	}
}
