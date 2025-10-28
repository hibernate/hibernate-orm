/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import java.util.List;

import org.hibernate.LazyInitializationException;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@DomainModel(
		annotatedPackageNames = "org.hibernate.orm.test.any.annotations",
		annotatedClasses = {
				StringProperty.class,
				IntegerProperty.class,
				LongProperty.class,
				PropertySet.class,
				LazyPropertySet.class,
				PropertyMap.class,
				PropertyList.class,
				PropertyHolder.class,
				CharProperty.class
		}
)
@SessionFactory
public class AnyTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						final PropertySet set1 = new PropertySet( "string" );
						final Property property = new StringProperty( "name", "Alex" );
						set1.setSomeProperty( property );
						set1.addGeneralProperty( property );
						session.persist( set1 );

						final PropertySet set2 = new PropertySet( "integer" );
						final Property property2 = new IntegerProperty( "age", 33 );
						set2.setSomeProperty( property2 );
						set2.addGeneralProperty( property2 );
						session.persist( set2 );
					}

					{
						final PropertyMap map = new PropertyMap( "sample" );
						map.getProperties().put( "name", new StringProperty( "name", "Alex" ) );
						map.getProperties().put( "age", new IntegerProperty( "age", 33 ) );
						session.persist( map );
					}

					{
						StringProperty nameProperty = new StringProperty( "name", "John Doe" );
						session.persist( nameProperty );

						PropertyHolder namePropertyHolder = new PropertyHolder();
						namePropertyHolder.setId( 1 );
						namePropertyHolder.setProperty( nameProperty );

						session.persist( namePropertyHolder );

						final IntegerProperty ageProperty = new IntegerProperty( "age", 23 );
						session.persist( ageProperty );

						PropertyHolder agePropertyHolder = new PropertyHolder();
						agePropertyHolder.setId( 2 );
						agePropertyHolder.setProperty( ageProperty );

						session.persist( agePropertyHolder );
					}

					{
						final PropertyList list = new PropertyList( "sample" );
						final StringProperty stringProperty = new StringProperty( "name", "Alex" );
						final IntegerProperty integerProperty = new IntegerProperty( "age", 33 );
						final LongProperty longProperty = new LongProperty( "distance", 121L );
						final CharProperty charProp = new CharProperty( "Est", 'E' );

						list.setSomeProperty( longProperty );

						list.addGeneralProperty( stringProperty );
						list.addGeneralProperty( integerProperty );
						list.addGeneralProperty( longProperty );
						list.addGeneralProperty( charProp );

						session.persist( list );
					}

					{
						final LazyPropertySet set = new LazyPropertySet( "string" );
						final Property property = new StringProperty( "name", "Alex" );
						set.setSomeProperty( property );
						session.persist( set );
					}
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-16732")
	public void testHqlAnyIdQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<PropertyHolder> list1 = session.createQuery(
							"select p from PropertyHolder p where id(p.property) = 666",
							PropertyHolder.class ).list();
					assertEquals( 0, list1.size() );
					List<PropertyHolder> list2 = session.createQuery(
							"select p from PropertyHolder p where type(p.property) = IntegerProperty",
							PropertyHolder.class ).list();
					assertEquals( 1, list2.size() );

				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15323")
	public void testHqlCollectionTypeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<PropertySet> propertySets = session.createQuery(
									"select p from PropertySet p where type(element(p.generalProperties)) = IntegerProperty ",
									PropertySet.class ).list();
					assertEquals( 1, propertySets.size() );

					PropertySet propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "age", propertySet.getGeneralProperties().get( 0 ).getName() );

					propertySets = session.createQuery(
									"select p from PropertySet p where type(element(p.generalProperties)) = StringProperty ",
									PropertySet.class ).list();
					assertEquals( 1, propertySets.size() );

					propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "name", propertySet.getGeneralProperties().get( 0 ).getName() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15442")
	public void testHqlCollectionTypeQueryWithParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<PropertySet> propertySets = session.createQuery(
									"select p from PropertySet p where type(element(p.generalProperties)) = :prop ",
									PropertySet.class )
							.setParameter( "prop", IntegerProperty.class)
							.list();
					assertEquals( 1, propertySets.size() );

					PropertySet propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "age", propertySet.getGeneralProperties().get( 0 ).getName() );

					propertySets = session.createQuery(
									"select p from PropertySet p where type(element(p.generalProperties)) = :prop ",
									PropertySet.class )
							.setParameter( "prop", StringProperty.class)
							.list();
					assertEquals( 1, propertySets.size() );

					propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "name", propertySet.getGeneralProperties().get( 0 ).getName() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15323")
	public void testHqlTypeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<PropertyHolder> propertyHolders = session.createQuery(
									"select p from PropertyHolder p where type(p.property) = IntegerProperty ",
									PropertyHolder.class ).list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "age", propertyHolders.get( 0 ).getProperty().getName() );

					propertyHolders = session.createQuery(
									"select p from PropertyHolder p where type(p.property) = StringProperty ",
									PropertyHolder.class ).list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "name", propertyHolders.get( 0 ).getProperty().getName() );
				}
		);
	}


	@Test
	@JiraKey( value = "HHH-15442")
	public void testHqlTypeQueryWithParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<PropertyHolder> propertyHolders = session.createQuery(
									"select p from PropertyHolder p where type(p.property) = :prop ",
									PropertyHolder.class )
							.setParameter( "prop", IntegerProperty.class)
							.list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "age", propertyHolders.get( 0 ).getProperty().getName() );

					propertyHolders = session.createQuery(
									"select p from PropertyHolder p where type(p.property) = :prop ",
									PropertyHolder.class )
							.setParameter( "prop", StringProperty.class)
							.list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "name", propertyHolders.get( 0 ).getProperty().getName() );
				}
		);
	}

	@Test
	public void testDefaultAnyAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<PropertySet> query = session.createQuery(
							"select s from PropertySet s where name = :name",
							PropertySet.class
					);

					{
						final PropertySet result = query.setParameter( "name", "string" ).uniqueResult();
						assertNotNull( result );
						assertNotNull( result.getSomeProperty() );
						assertTrue( result.getSomeProperty() instanceof StringProperty );
						assertEquals( "Alex", result.getSomeProperty().asString() );
						assertNotNull( result.getGeneralProperties() );
						assertEquals( 1, result.getGeneralProperties().size() );
						assertEquals( "Alex", result.getGeneralProperties().get( 0 ).asString() );
					}

					{
						final PropertySet result = query.setParameter( "name", "integer" ).uniqueResult();
						assertNotNull( result );
						assertNotNull( result.getSomeProperty() );
						assertTrue( result.getSomeProperty() instanceof IntegerProperty );
						assertEquals( "33", result.getSomeProperty().asString() );
						assertNotNull( result.getGeneralProperties() );
						assertEquals( 1, result.getGeneralProperties().size() );
						assertEquals( "33", result.getGeneralProperties().get( 0 ).asString() );
					}
				}
		);
	}

	@Test
	public void testManyToAnyWithMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final PropertyMap actualMap = session
							.createQuery( "SELECT m FROM PropertyMap m WHERE m.name = :name", PropertyMap.class )
							.setParameter( "name", "sample" )
							.uniqueResult();

					assertNotNull( actualMap );
					assertNotNull( actualMap.getProperties() );

					Property property = actualMap.getProperties().get( "name" );
					assertNotNull( property );
					assertTrue( property instanceof StringProperty );
					assertEquals( "Alex", property.asString() );

					property = actualMap.getProperties().get( "age" );
					assertNotNull( property );
					assertTrue( property instanceof IntegerProperty );
					assertEquals( "33", property.asString() );
				}
		);
	}

	@Test
	public void
	testMetaDataUseWithManyToAny(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//noinspection unchecked
					final PropertyList<Property> actualList = session
							.createQuery( "SELECT l FROM PropertyList l WHERE l.name = :name", PropertyList.class )
							.setParameter( "name", "sample" )
							.uniqueResult();

					assertNotNull( actualList );
					assertNotNull( actualList.getGeneralProperties() );
					assertEquals( 4, actualList.getGeneralProperties().size() );

					Property property = actualList.getSomeProperty();
					assertNotNull( property );
					assertTrue( property instanceof LongProperty );
					assertEquals( "121", property.asString() );

					assertEquals( "Alex", actualList.getGeneralProperties().get( 0 )
							.asString() );
					assertEquals( "33", actualList.getGeneralProperties().get( 1 ).asString() );
					assertEquals( "121", actualList.getGeneralProperties().get( 2 ).asString() );
					assertEquals( "E", actualList.getGeneralProperties().get( 3 ).asString() );
				}
		);
	}

	@Test
	public void testFetchEager(SessionFactoryScope scope) {
		final PropertySet result = scope.fromTransaction(
				session -> {
					final PropertySet localResult = session.createQuery(
									"select s from PropertySet s where name = :name",
									PropertySet.class
							)
							.setParameter( "name", "string" )
							.getSingleResult();
					assertNotNull( localResult );
					assertNotNull( localResult.getSomeProperty() );

					return localResult;
				}
		);

		assertTrue( result.getSomeProperty() instanceof StringProperty );
		assertEquals( "Alex", result.getSomeProperty().asString() );
	}

	@Test
	public void testFetchLazy(SessionFactoryScope scope) {
		final LazyPropertySet result = scope.fromTransaction(
				session -> {
					final LazyPropertySet localResult = session.createQuery(
									"select s from LazyPropertySet s where name = :name",
									LazyPropertySet.class
							)
							.setParameter( "name", "string" )
							.getSingleResult();
					assertNotNull( localResult );
					assertNotNull( localResult.getSomeProperty() );

					return localResult;
				}
		);

		try {
			result.getSomeProperty().asString();
			fail( "should not get the property string after session closed." );
		}
		catch (LazyInitializationException e) {
			// expected
		}
		catch (Exception e) {
			fail( "should not throw exception other than LazyInitializationException." );
		}
	}
}
