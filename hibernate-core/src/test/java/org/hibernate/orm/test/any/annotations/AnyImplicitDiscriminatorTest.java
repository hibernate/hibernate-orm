/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DomainModel(
		annotatedPackageNames = "org.hibernate.orm.test.any.annotations",
		annotatedClasses = {
				StringProperty.class,
				IntegerProperty.class,
				LongProperty.class,
				ImplicitPropertySet.class,
				ImplicitPropertyMap.class,
				ImplicitPropertyList.class,
				ImplicitPropertyHolder.class,
				CharProperty.class
		}
)
@SessionFactory
public class AnyImplicitDiscriminatorTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						final ImplicitPropertySet set1 = new ImplicitPropertySet( "string" );
						final Property property = new StringProperty( "name", "Alex" );
						set1.setSomeProperty( property );
						set1.addGeneralProperty( property );
						session.persist( set1 );

						final ImplicitPropertySet set2 = new ImplicitPropertySet( "integer" );
						final Property property2 = new IntegerProperty( "age", 33 );
						set2.setSomeProperty( property2 );
						set2.addGeneralProperty( property2 );
						session.persist( set2 );
					}

					{
						final ImplicitPropertyMap map = new ImplicitPropertyMap( "sample" );
						map.getProperties().put( "name", new StringProperty( "name", "Alex" ) );
						map.getProperties().put( "age", new IntegerProperty( "age", 33 ) );
						session.persist( map );
					}

					{
						StringProperty nameProperty = new StringProperty( "name", "John Doe" );
						session.persist( nameProperty );

						ImplicitPropertyHolder namePropertyHolder = new ImplicitPropertyHolder();
						namePropertyHolder.setId( 1 );
						namePropertyHolder.setProperty( nameProperty );

						session.persist( namePropertyHolder );

						final IntegerProperty ageProperty = new IntegerProperty( "age", 23 );
						session.persist( ageProperty );

						ImplicitPropertyHolder agePropertyHolder = new ImplicitPropertyHolder();
						agePropertyHolder.setId( 2 );
						agePropertyHolder.setProperty( ageProperty );

						session.persist( agePropertyHolder );
					}

					{
						final ImplicitPropertyList list = new ImplicitPropertyList( "sample" );
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
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete StringProperty" ).executeUpdate();
					session.createMutationQuery( "delete IntegerProperty" ).executeUpdate();
					session.createMutationQuery( "delete LongProperty" ).executeUpdate();
					session.createMutationQuery( "delete CharProperty" ).executeUpdate();

					session.createMutationQuery( "delete ImplicitPropertyHolder" ).executeUpdate();
					session.createMutationQuery( "delete ImplicitPropertyList" ).executeUpdate();
					session.createMutationQuery( "delete ImplicitPropertyMap" ).executeUpdate();
					session.createMutationQuery( "delete ImplicitPropertySet" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-16732")
	public void testHqlAnyIdQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ImplicitPropertyHolder> list1 = session.createQuery(
							"select p from ImplicitPropertyHolder p where id(p.property) = 666",
							ImplicitPropertyHolder.class ).list();
					assertEquals( 0, list1.size() );
					List<ImplicitPropertyHolder> list2 = session.createQuery(
							"select p from ImplicitPropertyHolder p where type(p.property) = IntegerProperty",
							ImplicitPropertyHolder.class ).list();
					assertEquals( 1, list2.size() );

				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15323")
	public void testHqlCollectionTypeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ImplicitPropertySet> propertySets = session.createQuery(
									"select p from ImplicitPropertySet p where type(element(p.generalProperties)) = IntegerProperty ",
							ImplicitPropertySet.class ).list();
					assertEquals( 1, propertySets.size() );

					ImplicitPropertySet propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "age", propertySet.getGeneralProperties().get( 0 ).getName() );

					propertySets = session.createQuery(
									"select p from ImplicitPropertySet p where type(element(p.generalProperties)) = StringProperty ",
							ImplicitPropertySet.class ).list();
					assertEquals( 1, propertySets.size() );

					propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "name", propertySet.getGeneralProperties().get( 0 ).getName() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15442")
	public void testHqlCollectionTypeQueryWithParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ImplicitPropertySet> propertySets = session.createQuery(
									"select p from ImplicitPropertySet p where type(element(p.generalProperties)) = :prop ",
									ImplicitPropertySet.class )
							.setParameter( "prop", IntegerProperty.class)
							.list();
					assertEquals( 1, propertySets.size() );

					ImplicitPropertySet propertySet = propertySets.get( 0 );
					assertEquals( 1, propertySet.getGeneralProperties().size() );

					assertEquals( "age", propertySet.getGeneralProperties().get( 0 ).getName() );

					propertySets = session.createQuery(
									"select p from ImplicitPropertySet p where type(element(p.generalProperties)) = :prop ",
									ImplicitPropertySet.class )
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
	@TestForIssue( jiraKey = "HHH-15323")
	public void testHqlTypeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ImplicitPropertyHolder> propertyHolders = session.createQuery(
									"select p from ImplicitPropertyHolder p where type(p.property) = IntegerProperty ",
							ImplicitPropertyHolder.class ).list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "age", propertyHolders.get( 0 ).getProperty().getName() );

					propertyHolders = session.createQuery(
									"select p from ImplicitPropertyHolder p where type(p.property) = StringProperty ",
							ImplicitPropertyHolder.class ).list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "name", propertyHolders.get( 0 ).getProperty().getName() );
				}
		);
	}


	@Test
	@TestForIssue( jiraKey = "HHH-15442")
	public void testHqlTypeQueryWithParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<ImplicitPropertyHolder> propertyHolders = session.createQuery(
									"select p from ImplicitPropertyHolder p where type(p.property) = :prop ",
									ImplicitPropertyHolder.class )
							.setParameter( "prop", IntegerProperty.class)
							.list();
					assertEquals( 1, propertyHolders.size() );

					assertEquals( "age", propertyHolders.get( 0 ).getProperty().getName() );

					propertyHolders = session.createQuery(
									"select p from ImplicitPropertyHolder p where type(p.property) = :prop ",
									ImplicitPropertyHolder.class )
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
					final QueryImplementor<ImplicitPropertySet> query = session.createQuery(
							"select s from ImplicitPropertySet s where name = :name",
							ImplicitPropertySet.class
					);

					{
						final ImplicitPropertySet result = query.setParameter( "name", "string" ).uniqueResult();
						assertNotNull( result );
						assertNotNull( result.getSomeProperty() );
						assertTrue( result.getSomeProperty() instanceof StringProperty );
						assertEquals( "Alex", result.getSomeProperty().asString() );
						assertNotNull( result.getGeneralProperties() );
						assertEquals( 1, result.getGeneralProperties().size() );
						assertEquals( "Alex", result.getGeneralProperties().get( 0 ).asString() );
					}

					{
						final ImplicitPropertySet result = query.setParameter( "name", "integer" ).uniqueResult();
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
					final ImplicitPropertyMap actualMap = session
							.createQuery( "SELECT m FROM ImplicitPropertyMap m WHERE m.name = :name", ImplicitPropertyMap.class )
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
					final ImplicitPropertyList<Property> actualList = session
							.createQuery( "SELECT l FROM ImplicitPropertyList l WHERE l.name = :name", ImplicitPropertyList.class )
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
		final ImplicitPropertySet result = scope.fromTransaction(
				session -> {
					final ImplicitPropertySet localResult = session.createQuery(
									"select s from ImplicitPropertySet s where name = :name",
									ImplicitPropertySet.class
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

}
