/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.LazyInitializationException;
import org.hibernate.query.spi.QueryImplementor;

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
						final PropertySet set1 = new PropertySet("string");
						final Property property = new StringProperty("name", "Alex");
						set1.setSomeProperty(property);
						set1.addGeneralProperty(property);
						session.save(set1);

						final PropertySet set2 = new PropertySet("integer");
						final Property property2 = new IntegerProperty("age", 33);
						set2.setSomeProperty(property2);
						set2.addGeneralProperty(property2);
						session.save(set2);
					}

					{
						final PropertyMap map = new PropertyMap("sample");
						map.getProperties().put("name", new StringProperty("name", "Alex"));
						map.getProperties().put("age", new IntegerProperty("age", 33));
						session.save(map);
					}

					{
						final PropertyList list = new PropertyList("sample");
						final StringProperty stringProperty = new StringProperty("name", "Alex");
						final IntegerProperty integerProperty = new IntegerProperty("age", 33);
						final LongProperty longProperty = new LongProperty("distance", 121L);
						final CharProperty charProp = new CharProperty("Est", 'E');

						list.setSomeProperty(longProperty);

						list.addGeneralProperty(stringProperty);
						list.addGeneralProperty(integerProperty);
						list.addGeneralProperty(longProperty);
						list.addGeneralProperty(charProp);

						session.save(list);
					}

					{
						final LazyPropertySet set = new LazyPropertySet( "string" );
						final Property property = new StringProperty( "name", "Alex" );
						set.setSomeProperty( property );
						session.save( set );
					}
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete StringProperty" ).executeUpdate();
					session.createQuery( "delete IntegerProperty" ).executeUpdate();
					session.createQuery( "delete LongProperty" ).executeUpdate();
					session.createQuery( "delete CharProperty" ).executeUpdate();

					session.createQuery( "delete PropertyList" ).executeUpdate();
					session.createQuery( "delete PropertyMap" ).executeUpdate();
					session.createQuery( "delete PropertySet" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDefaultAnyAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<PropertySet> query = session.createQuery("select s from PropertySet s where name = :name", PropertySet.class);

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
	public void testMetaDataUseWithManyToAny(SessionFactoryScope scope) {
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
					final PropertySet localResult = session.createQuery("select s from PropertySet s where name = :name", PropertySet.class)
							.setParameter("name", "string")
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
					final LazyPropertySet localResult = session.createQuery("select s from LazyPropertySet s where name = :name", LazyPropertySet.class)
							.setParameter("name", "string")
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
