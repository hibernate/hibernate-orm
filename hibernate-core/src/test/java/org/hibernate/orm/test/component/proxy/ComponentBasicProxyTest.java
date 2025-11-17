/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.proxy;

import java.util.List;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.spi.CompositeTypeImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Guillaume Smet
 * @author Oliver Libutzki
 */
@DomainModel( annotatedClasses = { Person.class, Adult.class } )
@SessionFactory
public class ComponentBasicProxyTest {

	@Test
	@JiraKey(value = "HHH-12786")
	public void testBasicProxyingWithProtectedMethodCalledInConstructor(SessionFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			Adult adult = new Adult();
			adult.setName( "Arjun Kumar" );
			entityManager.persist( adult );
		} );

		scope.inTransaction( (entityManager) -> {
			List<Adult> adultsCalledArjun = entityManager
					.createQuery( "SELECT a from Adult a WHERE a.name = :name", Adult.class )
					.setParameter( "name", "Arjun Kumar" ).getResultList();
			Adult adult = adultsCalledArjun.iterator().next();
			entityManager.remove( adult );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12791")
	public void testOnlyOneProxyClassGenerated(DomainModelScope domainModelScope, SessionFactoryScope sfScope) {
		sfScope.getSessionFactory();
		final PersistentClass personDescriptor = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		final CompositeTypeImplementor componentType = (CompositeTypeImplementor) personDescriptor.getIdentifierMapper().getType();
		final EmbeddableValuedModelPart embedded = componentType.getMappingModelPart();
		final EmbeddableInstantiator instantiator = embedded.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy()
				.getInstantiator();

		final Object instance1 = instantiator.instantiate( null );
		final Object instance2 = instantiator.instantiate( null );
		assertThat( instance1.getClass() ).isEqualTo( instance2.getClass() );
	}
}
