/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.immutable;

import jakarta.persistence.PersistenceException;

import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Burgel
 */
@ServiceRegistry(
		settings = {
				@Setting( name = Environment.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = Environment.USE_QUERY_CACHE, value = "true" ),
				@Setting( name = Environment.GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/naturalid/immutable/ParentChildWithManyToOne.hbm.xml" )
@SessionFactory
public class ImmutableManyToOneNaturalIdHbmTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Parent p = new Parent( 1, "alex" );
					Child c = new Child( 1, "billy", p );
					session.persist( p );
					session.persist( c );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-10360")
	public void checkingMapping(SessionFactoryScope scope) {

		final RuntimeMetamodels runtimeMetamodels = scope.getSessionFactory().getRuntimeMetamodels();
		final EntityMappingType childMapping = runtimeMetamodels.getEntityMappingType( Child.class.getName() );

		final EntityPersister persister = childMapping.getEntityPersister();
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		final int nameIndex = entityMetamodel.getPropertyIndex( "name" );
		final int parentIndex = entityMetamodel.getPropertyIndex( "parent" );

		// checking alphabetic sort in relation to EntityPersister/EntityMetamodel
		assertThat( nameIndex, lessThan( parentIndex ) );

		assertFalse( persister.getPropertyUpdateability()[ nameIndex ] );
		assertFalse( persister.getPropertyUpdateability()[ parentIndex ] );

		// nullability is not specified for either properties making up
		// the natural ID, so they should be non-nullable by hbm-specific default

		assertFalse( persister.getPropertyNullability()[ nameIndex ] );
		assertFalse( persister.getPropertyNullability()[ parentIndex ] );

		final NaturalIdMapping naturalIdMapping = childMapping.getNaturalIdMapping();
		assertNotNull( naturalIdMapping );
		assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );

		final SingularAttributeMapping first = naturalIdMapping.getNaturalIdAttributes().get( 0 );
		assertThat( first.getAttributeName(), is( "name" ) );
		final AttributeMetadata firstMetadata = first.getAttributeMetadata();
		assertFalse( firstMetadata.getMutabilityPlan().isMutable() );

		final SingularAttributeMapping second = naturalIdMapping.getNaturalIdAttributes().get( 1 );
		assertThat( second.getAttributeName(), is( "parent" ) );
		final AttributeMetadata secondMetadata = second.getAttributeMetadata();
		assertFalse( secondMetadata.getMutabilityPlan().isMutable() );
	}

	@Test
	public void testNaturalIdCheck(SessionFactoryScope scope) {
		final Child child = scope.fromTransaction( (s) -> s.get( Child.class, 1 ) );

		// child is detached...
		//   - change the name and attempt to reattach it, which should fail
		//		because name is defined as an immutable natural-id
		child.setName( "phil" );

		scope.inTransaction(
				(s) -> {
					try {
						s.merge( child );
						s.flush();
						fail( "should have failed because immutable natural ID was altered");
					}
					catch (PersistenceException e) {
						// expected
					}
				}
		);
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testSaveParentWithDetachedChildren(SessionFactoryScope scope) {
		final Parent p = scope.fromTransaction(
				(session) -> session.createQuery( "from Parent p join fetch p.children where p.name = 'alex'", Parent.class )
						.setCacheable( true )
						.uniqueResult()
		);

		// parent and its child are detached...
		//	- create a new child and associate it with the parent and reattach
		// 		NOTE : this fails if AbstractEntityPersister returns identifiers instead of entities from
		//		AbstractEntityPersister.getNaturalIdSnapshot()

		// todo (6.0) : ^^ this test has nothing to do with (im)mutability...

		Child c2 = new Child( 2, "joey", p );
		p.getChildren().add( c2 );

		scope.inTransaction( (session) -> session.merge( p ) );
	}

}
