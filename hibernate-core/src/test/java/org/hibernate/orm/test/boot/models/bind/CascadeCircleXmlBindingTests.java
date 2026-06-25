/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.List;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
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
	}
