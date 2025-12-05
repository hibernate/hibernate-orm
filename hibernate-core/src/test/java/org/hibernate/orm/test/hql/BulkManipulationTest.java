/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import junit.framework.AssertionFailedError;
import org.hibernate.QueryException;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.generator.Generator;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import static org.hibernate.cfg.MappingSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests execution of bulk UPDATE/DELETE statements through the new AST parser.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name=DEFAULT_LIST_SEMANTICS, value = "bag"))
@DomainModel(
		annotatedClasses = { Farm.class, Crop.class },
		xmlMappings = {
				"org/hibernate/orm/test/hql/Animal.hbm.xml",
				"org/hibernate/orm/test/hql/Vehicle.hbm.xml",
				"org/hibernate/orm/test/hql/KeyManyToOneEntity.hbm.xml",
				"org/hibernate/orm/test/hql/Versions.hbm.xml",
				"org/hibernate/orm/test/hql/FooBarCopy.hbm.xml",
				"org/hibernate/orm/test/hql/EntityWithCrazyCompositeKey.hbm.xml",
				"org/hibernate/orm/test/hql/SimpleEntityWithAssociation.hbm.xml",
				"org/hibernate/orm/test/hql/BooleanLiteralEntity.hbm.xml",
				"org/hibernate/orm/test/hql/CompositeIdEntity.hbm.xml",
				"/org/hibernate/orm/test/legacy/Multi.hbm.xml"
		}
)
@SessionFactory
public class BulkManipulationTest {

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testUpdateWithSubquery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// just checking parsing and syntax...
			session.createQuery( "update Human h set h.bodyWeight = h.bodyWeight + (select count(1) from IntegerVersioned)" )
					.executeUpdate();
			session.createQuery(
							"update Human h set h.bodyWeight = h.bodyWeight + (select count(1) from IntegerVersioned) where h.description = 'abc'" )
					.executeUpdate();
		} );
	}

	@Test
	public void testDeleteNonExistentEntity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "delete NonExistentEntity" ).executeUpdate();
				Assertions.fail( "no exception thrown" );
			}
			catch (IllegalArgumentException e) {
				assertTyping( QueryException.class, e.getCause() );
			}
			catch (QueryException ignore) {
			}
		} );
	}

	@Test
	public void testUpdateNonExistentEntity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "update NonExistentEntity e set e.someProp = ?" ).executeUpdate();
				Assertions.fail( "no exception thrown" );
			}
			catch (IllegalArgumentException e) {
				assertTyping( QueryException.class, e.getCause() );
			}
			catch (QueryException expected) {
			}
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	public void testTempTableGenerationIsolation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Truck truck = new Truck();
			truck.setVin( "123t" );
			truck.setOwner( "Steve" );
			session.persist( truck );

			// manually flush the session to ensure the insert happens
			session.flush();

			// now issue a bulk delete against Car which should force the temp table to be
			// created.  we need to test to ensure that this does not cause the transaction
			// to be committed...
			session.createMutationQuery( "delete from Vehicle" ).executeUpdate();
		} );

		factoryScope.inTransaction( (session) -> {
			var list = session.createQuery( "from Car" ).list();
			assertEquals( 0, list.size(), "temp table gen caused premature commit" );
		} );
	}

	@Test
	public void testBooleanHandling(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		// baseline check...
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select e from BooleanLiteralEntity e where e.yesNoBoolean = :p" )
					.setParameter( "p", true )
					.list();
		} );

		factoryScope.inTransaction( (session) -> {
			// currently, we need the three different binds because they are different underlying types...
			var hql = """
					update BooleanLiteralEntity
					set yesNoBoolean = :b1,
						trueFalseBoolean = :b2,
						zeroOneBoolean = :b3
					""";
			int count = session.createQuery( hql )
					.setParameter( "b1", true )
					.setParameter( "b2", true )
					.setParameter( "b3", true )
					.executeUpdate();
			assertEquals( 1, count );
			var entity = session.createQuery( "from BooleanLiteralEntity", BooleanLiteralEntity.class ).uniqueResult();
			assertTrue( entity.isYesNoBoolean() );
			assertTrue( entity.isTrueFalseBoolean() );
			assertTrue( entity.isZeroOneBoolean() );
			session.clear();

			hql = """
				update BooleanLiteralEntity
					set yesNoBoolean = true,
						trueFalseBoolean = true,
						zeroOneBoolean = true
				""";
			count = session.createQuery( hql ).executeUpdate();
			assertEquals( 1, count );

			entity = session.createQuery( "from BooleanLiteralEntity", BooleanLiteralEntity.class ).uniqueResult();
			assertTrue( entity.isYesNoBoolean() );
			assertTrue( entity.isTrueFalseBoolean() );
			assertTrue( entity.isZeroOneBoolean() );
		} );

		data.cleanup();
	}

	@Test
	public void testSimpleInsert(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			session.createQuery( "insert into Pickup (id, vin, owner) select id, vin, owner from Car" ).executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	public void testSelectWithNamedParamProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			session.createQuery( "select :someParameter, id from Car" ).setParameter( "someParameter", 1 ).getResultList();
		} );
	}

	@Test
	public void testSimpleInsertWithNamedParam(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			session.createQuery( "insert into Pickup (id, owner, vin) select id, :owner, vin from Car" )
					.setParameter( "owner", "owner" )
					.executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	@JiraKey( value = "HHH-15161")
	public void testInsertWithNullParamValue(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			session.createQuery( "insert into Pickup (id, owner, vin) select id, :owner, vin from Car" )
					.setParameter( "owner", null )
					.executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	@JiraKey( value = "HHH-15161")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not allow 'union' in 'insert select'")
	public void testInsertWithNullParamValueSetOperation(SessionFactoryScope  factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			var hql = """
					insert into Pickup (id, owner, vin)
					(select id, :owner, vin from Car union all select id, :owner, vin from Car)
					order by 1
					limit 1
					""";
			session.createQuery( hql )
					.setParameter( "owner", null )
					.executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	public void testInsertWithMultipleNamedParams(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			// Ensure this works by executing it
			session.createQuery( "insert into Pickup (id, owner, vin) select :id, owner, :vin from Car" )
					.setParameter( "id", 5l )
					.setParameter( "vin", "some" )
					.executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	public void testInsertWithSubqueriesAndNamedParams(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			var hql = """
					insert into Pickup (id, owner, vin)
					select :id, (select a.description from Animal a where a.description = :description), :vin
					from Car
					""";
			session.createQuery( hql )
					.setParameter( "id", 5l )
					.setParameter( "description", "Frog" )
					.setParameter( "vin", "some" )
					.executeUpdate();

			hql = """
				insert into Pickup (id, owner, vin)
				select :id,
					(select :description
						from Animal a
						where a.description = :description),
					:vin
				from Car
				""";
			session.createQuery( hql )
					.setParameter( "id", 10L )
					.setParameter( "description", "Frog" )
					.setParameter( "vin", "some" )
					.executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	public void testSimpleInsertTypeMismatchException(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				var hql = "insert into Pickup (id, owner, vin) select id, :owner, id from Car";
				session.createQuery( hql );
				Assertions.fail( "Parameter type mismatch but no exception thrown" );
			}
			catch (Throwable throwable) {
				QueryException queryException = assertTyping( QueryException.class, throwable.getCause() );
				String m = queryException.getMessage();
				// Expected insert attribute type [java.lang.String] did not match Query selection type [java.lang.Long] at selection index [2]
				int st = m.indexOf( "java.lang.String" );
				int lt = m.indexOf( "java.lang.Long" );
				assertTrue( st > -1, "type causing error not reported" );
				assertTrue( lt > -1, "type causing error not reported" );
				assertTrue( lt < st );
			}
		} );
	}

	@Test
	public void testSimpleNativeQueryManipulations(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		// insert
		factoryScope.inTransaction( (session) -> {
			List<?> l = session.createQuery( "from Vehicle" ).list();
			assertEquals( 4, l.size() );

			var sql = "insert into Pickup (id, vin, owner) select id * 10, vin, owner from Car";
			session.createNativeQuery( sql ).executeUpdate();

			l = session.createQuery( "from Vehicle" ).list();
			assertEquals( 5, l.size() );
		} );

		// deletes
		factoryScope.inTransaction( (session) -> {
			int deleteCount = session.createNativeQuery( "delete from Truck" ).executeUpdate();
			assertEquals( 1, deleteCount );

			List<?> l = session.createQuery( "from Vehicle" ).list();
			assertEquals( 4, l.size() );

			Car c = (Car) session.createQuery( "from Car where owner = 'Kirsten'" ).uniqueResult();
			c.setOwner( "NotKirsten" );
			assertEquals( 0,
					session.getNamedQuery( "native-delete-car" ).setParameter( 1, "Kirsten" ).executeUpdate() );
			assertEquals( 1,
					session.getNamedQuery( "native-delete-car" ).setParameter( 1, "NotKirsten" ).executeUpdate() );

			assertEquals( 0, session.createNativeQuery( "delete from SUV where owner = :owner" )
					.setParameter( "owner", "NotThere" )
					.executeUpdate() );
			assertEquals( 1, session.createNativeQuery( "delete from SUV where owner = :owner" )
					.setParameter( "owner", "Joe" )
					.executeUpdate() );

			session.createNativeQuery( "delete from Pickup" ).executeUpdate();
			l = session.createQuery( "from Vehicle" ).list();
			assertEquals( 0, l.size() );
		} );

		data.cleanup();
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTemporaryTableIdentity.class,
			comment = "The use of the native generator leads to using identity which also needs to be supported on temporary tables")
	@SkipForDialect( dialectClass = CockroachDialect.class,
			reason = "See https://hibernate.atlassian.net/browse/HHH-19332")
	public void testInsertWithManyToOne(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( Animal.class, factoryScope ),
				"bulk id generation not supported" );

		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			var hql = "insert into Animal (description, bodyWeight, mother) select description, bodyWeight, mother from Human";
			session.createQuery( hql ).executeUpdate();
		} );

		data.cleanup();
	}

	@Test
	public void testInsertWithMismatchedTypes(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			try {
				session.createQuery( "insert into Pickup (owner, vin, id) select id, vin, owner from Car" ).executeUpdate();
				Assertions.fail( "mismatched types did not error" );
			}
			catch (IllegalArgumentException e) {
				assertTyping( QueryException.class, e.getCause() );
			}
			catch (QueryException e) {
				// expected result
			}
		} );

		data.cleanup();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "https://github.com/cockroachdb/cockroach/issues/75101")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class,
			matchSubTypes = true,
			reason = "T-SQL complains IDENTITY_INSERT is off when a value for an identity column is provided")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTableIdentity.class,
			comment = "The use of the native generator leads to using identity which also needs to be supported on temporary tables")
	public void testInsertIntoSuperclassPropertiesFails(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			session.createQuery( "insert into Human (id, bodyWeight, heightInches) select id * 10, bodyWeight, 180D from Cat" ).executeUpdate();
			List<Number> list = session.createNativeQuery( "select height_centimeters from Human" ).getResultList();
			assertEquals( 1, list.size() );
			assertEquals( 180, list.get( 0 ).doubleValue(), 0.01 );
		} );

		data.cleanup();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTableIdentity.class,
			comment = "The use of the native generator leads to using identity which also needs to be supported on temporary tables")
	public void testInsertAcrossMappedJoin(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( Joiner.class, factoryScope ),
				"bulk id generation not supported" );

		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			session.createQuery( "insert into Joiner (name, joinedName) select vin, owner from Car" ).executeUpdate();
			Joiner joiner = session.createQuery( "from Joiner where name = '123c'", Joiner.class ).uniqueResult();
			assertEquals( "Kirsten", joiner.getJoinedName() );
		} );

		data.cleanup();
	}

	protected boolean supportsBulkInsertIdGeneration(Class entityClass, SessionFactoryScope factoryScope) {
		EntityPersister persister = factoryScope
				.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(entityClass.getName());
		Generator generator = persister.getGenerator();
		return generator instanceof BulkInsertionCapableIdentifierGenerator
			&& ( (BulkInsertionCapableIdentifierGenerator) generator ).supportsBulkInsertionIdentifierGeneration();
	}

	@Test
	public void testInsertWithGeneratedId(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( PettingZoo.class, factoryScope ),
				"bulk id generation not supported" );

		// create a Zoo
		var zoo = factoryScope.fromTransaction( (session) -> {
			var z = new Zoo( "zoo", null );
			session.persist( z );
			return z;
		} );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "insert into PettingZoo (name) select name from Zoo" ).executeUpdate();
			assertEquals( 1, count, "unexpected insertion count" );
		} );

		factoryScope.inTransaction( (session) -> {
			var pettingZoo = session.createQuery( "from PettingZoo", PettingZoo.class ).uniqueResult();
			assertEquals( zoo.getName(), pettingZoo.getName() );
			assertTrue( !zoo.getId().equals( pettingZoo.getId() ) );
		} );
	}

	@Test
	public void testInsertWithGeneratedVersionAndId(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( IntegerVersioned.class, factoryScope ),
				"bulk id generation not supported" );

		var initial = factoryScope.fromTransaction( (session) -> {
			var entity = new IntegerVersioned( "int-vers" );
			session.persist( entity );
			session.createQuery( "select id, name, version from IntegerVersioned" ).list();
			return entity;
		} );

		Long initialId = initial.getId();
		int initialVersion = initial.getVersion();

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "insert into IntegerVersioned ( name ) select name from IntegerVersioned" )
					.executeUpdate();
			assertEquals( 1, count, "unexpected insertion count" );
		} );

		var queried = factoryScope.fromTransaction( (session) -> {
			return session.createQuery( "from IntegerVersioned where id <> :initialId", IntegerVersioned.class )
					.setParameter( "initialId", initialId.longValue() )
					.uniqueResult();
		} );

		assertEquals( initialVersion, queried.getVersion(), "version was not seeded" );
	}

	@Test
	public void testInsertWithGeneratedTimestampVersion(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( TimestampVersioned.class, factoryScope ),
				"bulk id generation not supported" );

		var created = factoryScope.fromTransaction( (session) -> {
			TimestampVersioned entity = new TimestampVersioned( "int-vers" );
			session.persist( entity );
			session.createQuery( "select id, name, version from TimestampVersioned" ).list();
			return entity;
		} );

		var initialId = created.getId();

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "insert into TimestampVersioned ( name ) select name from TimestampVersioned" )
					.executeUpdate();
			assertEquals( 1, count, "unexpected insertion count" );
		} );


		factoryScope.inTransaction( (session) -> {
			final TimestampVersioned inserted = session.createQuery(
							"from TimestampVersioned where id <> :initialId", TimestampVersioned.class )
					.setParameter( "initialId", initialId.longValue() )
					.uniqueResult();
			Assertions.assertNotNull( inserted.getVersion() );
		} );
	}

	@Test
	public void testInsertWithAssignedCompositeId(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// this just checks that the query parser detects that we are explicitly inserting a composite id
			// intentionally reversing the order of the composite id properties to make sure that is supported too
			session.createQuery(
							"insert into CompositeIdEntity (key2, someProperty, key1) select a.key2, 'COPY', a.key1 from CompositeIdEntity a" )
					.executeUpdate();
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTableIdentity.class,
			comment = "The use of the native generator leads to using identity which also needs to be supported on temporary tables")
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "See https://hibernate.atlassian.net/browse/HHH-19332")
	public void testInsertWithSelectListUsingJoins(SessionFactoryScope factoryScope) {
		// Make sure the env supports bulk inserts with generated ids...
		Assumptions.assumeTrue( supportsBulkInsertIdGeneration( Animal.class, factoryScope ),
				"bulk id generation not supported" );

		factoryScope.inTransaction( (session) -> {
			// this is just checking parsing and syntax...
			var hql = """
					insert into Animal (description, bodyWeight)
					select h.description, h.bodyWeight
					from Human h
					where h.mother.mother is not null
					""";
			session.createQuery( hql ).executeUpdate();
			hql = """
				insert into Animal (description, bodyWeight)
				select h.description, h.bodyWeight
				from Human h
					join h.mother m
				where m.mother is not null
				""";
			session.createQuery( hql ).executeUpdate();
		} );
	}

	@Test
	public void testIncorrectSyntax(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				var hql = "update Human set Human.description = 'xyz' where Human.id = 1 and Human.description is null";
				session.createQuery( hql ).executeUpdate();
				Assertions.fail( "expected failure" );
			}
			catch (IllegalArgumentException e) {
				assertTyping( QueryException.class, e.getCause() );
			}
			catch (QueryException expected) {
				// ignore : expected behavior
			}
		} );
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateWithWhereExistsSubquery(SessionFactoryScope factoryScope) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// multi-table
		factoryScope.inTransaction( (session) -> {
			var joe = new Human();
			joe.setName( new Name( "Joe", 'Q', "Public" ) );
			session.persist( joe );
			var doll = new Human();
			doll.setName( new Name( "Kyu", 'P', "Doll" ) );
			doll.setFriends( new ArrayList() );
			doll.getFriends().add( joe );
			session.persist( doll );
		} );
		factoryScope.inTransaction( (session) -> {
			var hql = """
					update Human h
					set h.description = 'updated'
					where exists (
						select f.id
						from h.friends f
						where f.name.last = 'Public'
					)
					""";
			int count = session.createQuery( hql ).executeUpdate();
			assertEquals( 1, count );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// single-table (one-to-many & many-to-many)
		factoryScope.inTransaction( (session) -> {
			var entity = new SimpleEntityWithAssociation();
			var other = new SimpleEntityWithAssociation();
			entity.setName( "main" );
			other.setName( "many-to-many-association" );
			entity.getManyToManyAssociatedEntities().add( other );
			entity.addAssociation( "one-to-many-association" );
			session.persist( entity );
		} );

		factoryScope.inTransaction( (session) -> {
			var hql = """
				update SimpleEntityWithAssociation e \
				set e.name = 'updated' \
				where exists (\
					select a.id \
					from e.associatedEntities a \
					where a.name = 'one-to-many-association' \
				)""";
			var count = session.createQuery( hql ).executeUpdate();
			assertEquals( 1, count );

			// one-to-many test
			// many-to-many test
			if ( session.getDialect().supportsSubqueryOnMutatingTable() ) {
				hql = """
						update SimpleEntityWithAssociation e \
						set e.name = 'updated' \
						where exists (\
							select a.id \
							from e.manyToManyAssociatedEntities a \
							where a.name = 'many-to-many-association' \
						)""";
				count = session.createQuery( hql ).executeUpdate();
				assertEquals( 1, count );
			}
		} );
	}

	@Test
	public void testIncrementCounterVersion(SessionFactoryScope factoryScope) {
		var entity = factoryScope.fromTransaction( (session) -> {
			var created = new IntegerVersioned( "int-vers" );
			session.persist( created );
			return created;
		} );

		int initialVersion = entity.getVersion();

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update versioned IntegerVersioned set name = name" ).executeUpdate();
			assertEquals( 1, count, "incorrect exec count" );
		} );

		factoryScope.inTransaction( (session) -> {
			var loaded = session.getReference( IntegerVersioned.class, entity.getId() );
			assertEquals( initialVersion + 1, loaded.getVersion(), "version not incremented" );
		} );
	}

	@Test
	public void testIncrementTimestampVersion(SessionFactoryScope factoryScope) {
		var entity = factoryScope.fromTransaction( (session) -> {
			var created = new TimestampVersioned( "ts-vers" );
			session.persist(  created );
			return created;
		} );

		Date initialVersion = entity.getVersion();

		synchronized (this) {
			try {
				wait( 1500 );
			}
			catch (InterruptedException ignored) {
			}
		}

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update versioned TimestampVersioned set name = name" ).executeUpdate();
			assertEquals( 1, count, "incorrect exec count" );
		} );

		factoryScope.inTransaction( (session) -> {
			var updated = session.getReference( TimestampVersioned.class, entity.getId() );
			assertTrue( updated.getVersion().after( initialVersion ), "version not incremented" );
		} );
	}

	@Test
	public void testUpdateOnComponent(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var human = new Human();
			human.setName( new Name( "Stevee", 'X', "Ebersole" ) );
			session.persist( human );
			session.flush();

			String correctName = "Steve";
			int count = session.createQuery( "update Human set name.first = :correction where id = :id" )
					.setParameter( "correction", correctName )
					.setParameter( "id", human.getId() )
					.executeUpdate();
			assertEquals( 1, count,
					"Incorrect update count" );

			session.refresh( human );
			assertEquals( correctName, human.getName().getFirst(),
					"Update did not execute properly" );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	public void testUpdateOnManyToOne(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "update Animal a set a.mother = null where a.id = 2" ).executeUpdate();
			if ( !( session.getDialect() instanceof MySQLDialect ) ) {
				// MySQL does not support (even un-correlated) subqueries against the update-mutating table
				session.createQuery( "update Animal a set a.mother = (from Animal where id = 1) where a.id = 2" ).executeUpdate();
			}
		} );
	}

	@Test
	public void testUpdateOnImplicitJoinFails(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			try {
				session.createQuery( "update Human set mother.name.initial = :initial" ).setParameter(
						"initial",
						'F'
				).executeUpdate();
				Assertions.fail( "update allowed across implicit join" );
			}
			catch (IllegalArgumentException e) {
				assertTyping( QueryException.class, e.getCause() );
			}
			catch (QueryException expected) {
			}
		} );
	}

	@Test
	public void testUpdateOnDiscriminatorSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update PettingZoo set name = name" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass update count" );

			session.getTransaction().rollback();
			session.getTransaction().begin();

			count = session.createQuery( "update PettingZoo pz set pz.name = pz.name where pz.id = :id" )
					.setParameter( "id", data.pettingZoo.getId() )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass update count" );

			session.getTransaction().rollback();
			session.getTransaction().begin();

			count = session.createQuery( "update Zoo as z set z.name = z.name" ).executeUpdate();
			assertEquals( 2, count, "Incorrect discrim subclass update count" );

			session.getTransaction().rollback();
			session.getTransaction().begin();

			// TODO : not so sure this should be allowed.
			//  	Seems to me that if they specify an alias, property-refs should be required to be qualified.
			count = session.createQuery( "update Zoo as z set name = name where id = :id" )
					.setParameter( "id", data.zoo.getId().longValue() )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass update count" );
		} );

		data.cleanup();
	}

	@Test
	public void testUpdateOnAnimal(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update Animal set description = description where description = :desc" )
					.setParameter( "desc", data.frog.getDescription() )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect entity-updated count" );

			count = session.createQuery( "update Animal set description = :newDesc where description = :desc" )
					.setParameter( "desc", data.polliwog.getDescription() )
					.setParameter( "newDesc", "Tadpole" )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect entity-updated count" );

			var tadpole = session.getReference( Animal.class, data.polliwog.getId() );
			assertEquals( "Tadpole", tadpole.getDescription(), "Update did not take effect" );

			count = session.createQuery( "update Animal set bodyWeight = bodyWeight + :w1 + :w2" )
					.setParameter( "w1", 1 )
					.setParameter( "w2", 2 )
					.executeUpdate();
			assertEquals( 6, count, "incorrect count on 'complex' update assignment" );

			if ( !( session.getDialect() instanceof MySQLDialect ) ) {
				// MySQL does not support (even un-correlated) subqueries against the update-mutating table
				session.createQuery( "update Animal set bodyWeight = ( select max(bodyWeight) from Animal )" )
						.executeUpdate();
			}
		} );

		data.cleanup();
	}

	@Test
	public void testUpdateOnMammal(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update Mammal set description = description" ).executeUpdate();
			assertEquals( 2, count, "incorrect update count against 'middle' of joined-subclass hierarchy" );

			count = session.createQuery( "update Mammal set bodyWeight = 25" ).executeUpdate();
			assertEquals( 2, count, "incorrect update count against 'middle' of joined-subclass hierarchy" );

			if ( !( session.getDialect() instanceof MySQLDialect ) ) {
				// MySQL does not support (even un-correlated) subqueries against the update-mutating table
				count = session.createQuery( "update Mammal set bodyWeight = ( select max(bodyWeight) from Animal )" )
						.executeUpdate();
				assertEquals( 2, count,
						"incorrect update count against 'middle' of joined-subclass hierarchy" );
			}
		} );

		data.cleanup();
	}

	@Test
	public void testUpdateSetNullUnionSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare(  factoryScope );

		factoryScope.inTransaction( (session) -> {
			// These should reach out into *all* subclass tables...
			int count = session.createQuery( "update Vehicle set owner = 'Steve'" ).executeUpdate();
			assertEquals( 4, count, "incorrect restricted update count" );
			count = session.createQuery( "update Vehicle set owner = null where owner = 'Steve'" ).executeUpdate();
			assertEquals( 4, count, "incorrect restricted update count" );

			try {
				count = session.createQuery( "delete Vehicle where owner is null" ).executeUpdate();
				assertEquals( 4, count, "incorrect restricted delete count" );
			}
			catch (AssertionFailedError afe) {
				if ( H2Dialect.class.isInstance( session.getDialect() ) ) {
					// http://groups.google.com/group/h2-database/t/5548ff9fd3abdb7
					// this is fixed in H2 1.2.140
					count = session.createQuery( "delete Vehicle" ).executeUpdate();
					assertEquals( 4, count, "incorrect count" );
				}
				else {
					throw afe;
				}
			}
		} );

		data.cleanup();
	}

	@Test
	public void testUpdateSetNullOnDiscriminatorSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare(   factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update PettingZoo set address.city = null" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );

			count = session.createQuery( "delete Zoo where address.city is null" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );

			count = session.createQuery( "update Zoo set address.city = null" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );

			count = session.createQuery( "delete Zoo where address.city is null" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );

		} );

		data.cleanup();
	}

	@Test
	public void testUpdateSetNullOnJoinedSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "update Mammal set bodyWeight = null" ).executeUpdate();
			assertEquals( 2, count, "Incorrect deletion count on joined subclass" );

			count = session.createQuery( "delete Animal where bodyWeight is null" ).executeUpdate();
			assertEquals( 2, count, "Incorrect deletion count on joined subclass" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteWithSubquery(SessionFactoryScope factoryScope) {
		// setup the test data...
		factoryScope.inTransaction( (session) -> {
			SimpleEntityWithAssociation owner = new SimpleEntityWithAssociation( "myEntity-1" );
			owner.addAssociation( "assoc-1" );
			owner.addAssociation( "assoc-2" );
			owner.addAssociation( "assoc-3" );
			session.persist( owner );
			SimpleEntityWithAssociation owner2 = new SimpleEntityWithAssociation( "myEntity-2" );
			owner2.addAssociation( "assoc-1" );
			owner2.addAssociation( "assoc-2" );
			owner2.addAssociation( "assoc-3" );
			owner2.addAssociation( "assoc-4" );
			session.persist( owner2 );
			SimpleEntityWithAssociation owner3 = new SimpleEntityWithAssociation( "myEntity-3" );
			session.persist( owner3 );
		} );

		// now try the bulk delete
		factoryScope.inTransaction( (session) -> {
			var queryString = """
					delete SimpleEntityWithAssociation e
					where size( e.associatedEntities ) = 0
						and e.name like '%'
					""";
			int count = session.createMutationQuery( queryString ).executeUpdate();
			assertEquals( 1, count, "incorrect delete count" );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.HasSelfReferentialForeignKeyBugCheck.class,
			comment = "self referential FK bug"
	)
	public void testSimpleDeleteOnAnimal(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete from Animal as a where a.id = :id" )
					.setParameter( "id", data.polliwog.getId().longValue() )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect delete count" );

			count = session.createQuery( "delete Animal where id = :id" )
					.setParameter( "id", data.catepillar.getId().longValue() )
					.executeUpdate();
			assertEquals( 1, count, "incorrect delete count" );

			if ( session.getDialect().supportsSubqueryOnMutatingTable() ) {
				count = session.createQuery( "delete from User u where u not in (select u from User u)" ).executeUpdate();
				assertEquals( 0, count );
			}

			count = session.createQuery( "delete Animal a" ).executeUpdate();
			assertEquals( 4, count, "Incorrect delete count" );

			List<?> list = session.createQuery( "select a from Animal as a" ).list();
			assertTrue( list.isEmpty(), "table not empty" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteOnDiscriminatorSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete PettingZoo" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );

			count = session.createQuery( "delete Zoo" ).executeUpdate();
			assertEquals( 1, count, "Incorrect discrim subclass delete count" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteOnJoinedSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope);

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete Mammal where bodyWeight > 150" ).executeUpdate();
			assertEquals( 1, count, "Incorrect deletion count on joined subclass" );

			count = session.createQuery( "delete Mammal" ).executeUpdate();
			assertEquals( 1, count, "Incorrect deletion count on joined subclass" );

			count = session.createQuery( "delete SubMulti" ).executeUpdate();
			assertEquals( 0, count, "Incorrect deletion count on joined subclass" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteOnMappedJoin(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete Joiner where joinedName = :joinedName" )
					.setParameter( "joinedName", "joined-name" )
					.executeUpdate();
			assertEquals( 1, count, "Incorrect deletion count on joined subclass" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteUnionSubclassAbstractRoot(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope);

		// These should reach out into *all* subclass tables...
		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete Vehicle where owner = :owner" )
					.setParameter( "owner", "Steve" )
					.executeUpdate();
			assertEquals( 1, count, "incorrect restricted update count" );

			count = session.createQuery( "delete Vehicle" ).executeUpdate();
			assertEquals( 3, count, "incorrect update count" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteUnionSubclassConcreteSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		// These should only affect the given table
		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete Truck where owner = :owner" )
					.setParameter( "owner", "Steve" )
					.executeUpdate();
			assertEquals( 1, count, "incorrect restricted update count" );

			count = session.createQuery( "delete Truck" ).executeUpdate();
			assertEquals( 2, count, "incorrect update count" );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteUnionSubclassLeafSubclass(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		// These should only affect the given table
		factoryScope.inTransaction( (session) -> {
			int count = session.createQuery( "delete Car where owner = :owner" )
					.setParameter( "owner", "Kirsten" )
					.executeUpdate();
			assertEquals( 1, count, "incorrect restricted update count" );

			count = session.createQuery( "delete Car" ).executeUpdate();
			assertEquals( 0, count, "incorrect update count" );
		} );

		data.cleanup();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	public void testDeleteWithMetadataWhereFragments(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// Note: we are just checking the syntax here...
			session.createQuery( "delete from Bar" ).executeUpdate();
			session.createQuery( "delete from Bar where barString = 's'" ).executeUpdate();
		} );
	}

	@Test
	public void testDeleteRestrictedOnManyToOne(SessionFactoryScope factoryScope) {
		TestData data = new TestData();
		data.prepare( factoryScope );

		factoryScope.inTransaction( (s) -> {
			int count = s.createQuery( "delete Animal where mother = :mother" )
					.setParameter( "mother", data.butterfly )
					.executeUpdate();
			assertEquals( 1, count );
		} );

		data.cleanup();
	}

	@Test
	public void testDeleteSyntaxWithCompositeId(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction((session) -> {
			session.createQuery( "delete EntityWithCrazyCompositeKey where id.id = 1 and id.otherId = 2" )
					.executeUpdate();
			session.createQuery( "delete from EntityWithCrazyCompositeKey where id.id = 1 and id.otherId = 2" )
					.executeUpdate();
			session.createQuery( "delete from EntityWithCrazyCompositeKey e where e.id.id = 1 and e.id.otherId = 2" )
					.executeUpdate();
		} );
	}

	@Test
	@JiraKey(value = "HHH-8476")
	public void testManyToManyBulkDelete(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var farm1 = new Farm();
			farm1.setName( "farm1" );
			var crop = new Crop();
			crop.setName( "crop1" );
			farm1.setCrops( new ArrayList() );
			farm1.getCrops().add( crop );
			session.persist( farm1 );

			var farm2 = new Farm();
			farm2.setName( "farm2" );
			farm2.setCrops( new ArrayList() );
			farm2.getCrops().add( crop );
			session.persist( farm2 );

			session.flush();

			try {
				session.createQuery( "delete from Farm f where f.name='farm1'" ).executeUpdate();
				assertEquals( 1, session.createQuery( "from Farm" ).list().size() );
				session.createQuery( "delete from Farm" ).executeUpdate();
				assertEquals( 0, session.createQuery( "from Farm" ).list().size() );
			}
			catch (ConstraintViolationException cve) {
				Assertions.fail( "The join table was not cleared prior to the bulk delete." );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-1917")
	public void testManyToManyBulkDeleteMultiTable(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var friend = new Human();
			friend.setName( new Name( "Bob", 'B', "Bobbert" ) );
			session.persist( friend );

			var brett = new Human();
			brett.setName( new Name( "Brett", 'E', "Meyer" ) );
			brett.setFriends( new ArrayList() );
			brett.getFriends().add( friend );
			session.persist( brett );

			session.flush();

			try {
				// multitable (joined subclass)
				session.createQuery( "delete from Human" ).executeUpdate();
				assertEquals( 0, session.createQuery( "from Human" ).list().size() );
			}
			catch (ConstraintViolationException cve) {
				Assertions.fail( "The join table was not cleared prior to the bulk delete." );
			}
		} );
	}

	@Test
	public void testBulkDeleteOfEntityWithElementCollection(SessionFactoryScope factoryScope) {
		// set up test data
		factoryScope.inTransaction((session) -> {
			var farm = new Farm();
			farm.setName( "Old McDonald Farm 'o the Earth" );
			farm.setAccreditations( new HashSet<>() );
			farm.getAccreditations().add( Farm.Accreditation.ORGANIC );
			farm.getAccreditations().add( Farm.Accreditation.SUSTAINABLE );
			session.persist( farm );
		} );


		// assertion that accreditations collection table got populated
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			final ResultSet resultSet = statement.executeQuery(
					"select count(*) from farm_accreditations" );
			assertTrue( resultSet.next() );
			final int count = resultSet.getInt( 1 );
			assertEquals( 2, count );
		} ) );

		// do delete
		factoryScope.inTransaction( (session) -> session.createQuery( "delete Farm" ).executeUpdate() );

		// assertion that accreditations collection table got cleaned up
		//		if they didn't, the delete should have caused a constraint error, but just to be sure...
		factoryScope.inTransaction((s) -> s.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			final ResultSet resultSet = statement.executeQuery(
					"select count(*) from farm_accreditations" );
			assertTrue( resultSet.next() );
			final int count = resultSet.getInt( 1 );
			assertEquals( 0, count );
		} ) );
	}

	@Test
	public void testBulkDeleteOfMultiTableEntityWithElementCollection(SessionFactoryScope factoryScope) {
		// set up test data
		factoryScope.inTransaction( (s) -> {
			var human = new Human();
			human.setNickNames( new TreeSet() );
			human.getNickNames().add( "Johnny" );
			s.persist( human );
		} );

		// assertion that nickname collection table got populated
		factoryScope.inTransaction((s) -> s.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			final ResultSet resultSet = statement.executeQuery( "select count(*) from human_nick_names" );
			assertTrue( resultSet.next() );
			final int count = resultSet.getInt( 1 );
			assertEquals( 1, count );
		} ) );

		// do delete
		factoryScope.inTransaction(s -> s.createQuery( "delete Human" ).executeUpdate() );

		// assertion that nickname collection table got cleaned up
		//		if they didn't, the delete should have caused a constraint error, but just to be sure...
		factoryScope.inTransaction( s -> s.doWork( (connection) -> {
			final Statement statement = connection.createStatement();
			final ResultSet resultSet = statement.executeQuery( "select count(*) from human_nick_names" );
			assertTrue( resultSet.next() );
			final int count = resultSet.getInt( 1 );
			assertEquals( 0, count );
		} ) );
	}


	private static class TestData {
		private Animal polliwog;
		private Animal catepillar;
		private Animal frog;
		private Animal butterfly;

		private Zoo zoo;
		private Zoo pettingZoo;

		private void prepare(SessionFactoryScope factoryScope) {
			factoryScope.inTransaction(s -> {
				polliwog = new Animal();
				polliwog.setBodyWeight( 12 );
				polliwog.setDescription( "Polliwog" );

				catepillar = new Animal();
				catepillar.setBodyWeight( 10 );
				catepillar.setDescription( "Catepillar" );

				frog = new Animal();
				frog.setBodyWeight( 34 );
				frog.setDescription( "Frog" );

				polliwog.setFather( frog );
				frog.addOffspring( polliwog );

				butterfly = new Animal();
				butterfly.setBodyWeight( 9 );
				butterfly.setDescription( "Butterfly" );

				catepillar.setMother( butterfly );
				butterfly.addOffspring( catepillar );

				s.persist( frog );
				s.persist( polliwog );
				s.persist( butterfly );
				s.persist( catepillar );

				Dog dog = new Dog();
				dog.setBodyWeight( 200 );
				dog.setDescription( "dog" );
				s.persist( dog );

				Cat cat = new Cat();
				cat.setBodyWeight( 100 );
				cat.setDescription( "cat" );
				s.persist( cat );

				zoo = new Zoo();
				zoo.setName( "Zoo" );
				Address add = new Address();
				add.setCity( "MEL" );
				add.setCountry( "AU" );
				add.setStreet( "Main st" );
				add.setPostalCode( "3000" );
				zoo.setAddress( add );

				pettingZoo = new PettingZoo();
				pettingZoo.setName( "Petting Zoo" );
				Address addr = new Address();
				addr.setCity( "Sydney" );
				addr.setCountry( "AU" );
				addr.setStreet( "High st" );
				addr.setPostalCode( "2000" );
				pettingZoo.setAddress( addr );

				s.persist( zoo );
				s.persist( pettingZoo );

				Joiner joiner = new Joiner();
				joiner.setJoinedName( "joined-name" );
				joiner.setName( "name" );
				s.persist( joiner );

				Car car = new Car();
				car.setVin( "123c" );
				car.setOwner( "Kirsten" );
				s.persist( car );

				Truck truck = new Truck();
				truck.setVin( "123t" );
				truck.setOwner( "Steve" );
				s.persist( truck );

				SUV suv = new SUV();
				suv.setVin( "123s" );
				suv.setOwner( "Joe" );
				s.persist( suv );

				Pickup pickup = new Pickup();
				pickup.setVin( "123p" );
				pickup.setOwner( "Cecelia" );
				s.persist( pickup );

				BooleanLiteralEntity bool = new BooleanLiteralEntity();
				s.persist( bool );
			} );
		}

		private void cleanup() {
			// do nothing - rely on the AfterEach callback to drop data
		}
	}
}
