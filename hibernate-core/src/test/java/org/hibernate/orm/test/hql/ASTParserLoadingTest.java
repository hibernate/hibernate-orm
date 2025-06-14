/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.assertj.core.data.Offset;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.TypeMismatchException;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.orm.test.any.xml.IntegerPropertyValue;
import org.hibernate.orm.test.any.xml.PropertySet;
import org.hibernate.orm.test.any.xml.PropertyValue;
import org.hibernate.orm.test.any.xml.StringPropertyValue;
import org.hibernate.orm.test.cid.Customer;
import org.hibernate.orm.test.cid.LineItem;
import org.hibernate.orm.test.cid.LineItem.Id;
import org.hibernate.orm.test.cid.Order;
import org.hibernate.orm.test.cid.Product;
import org.hibernate.query.Query;
import org.hibernate.query.SyntaxException;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertClassAssignability;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * Tests the integration of the new AST parser into the loading of query results using
 * the Hibernate persisters and loaders.
 * <p>
 * Also used to test the syntax of the resulting sql against the underlying
 * database, specifically for functionality not supported by the classic
 * parser.
 *
 * @author Steve
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(
		xmlMappings = {
				"/org/hibernate/orm/test/hql/Animal.hbm.xml",
				"/org/hibernate/orm/test/hql/FooBarCopy.hbm.xml",
				"/org/hibernate/orm/test/hql/SimpleEntityWithAssociation.hbm.xml",
				"/org/hibernate/orm/test/hql/CrazyIdFieldNames.hbm.xml",
				"/org/hibernate/orm/test/hql/Image.hbm.xml",
				"/org/hibernate/orm/test/hql/ComponentContainer.hbm.xml",
				"/org/hibernate/orm/test/hql/VariousKeywordPropertyEntity.hbm.xml",
				"/org/hibernate/orm/test/hql/Constructor.hbm.xml",
				"/org/hibernate/orm/test/batchfetch/ProductLine.xml",
				"/org/hibernate/orm/test/cid/Customer.hbm.xml",
				"/org/hibernate/orm/test/cid/Order.hbm.xml",
				"/org/hibernate/orm/test/cid/LineItem.hbm.xml",
				"/org/hibernate/orm/test/cid/Product.hbm.xml",
				"/org/hibernate/orm/test/any/xml/Properties.xml",
				"/org/hibernate/orm/test/legacy/Commento.hbm.xml",
				"/org/hibernate/orm/test/legacy/Marelo.hbm.xml"
		},
		annotatedClasses = {
				Department.class,
				Employee.class,
				Title.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
		}
)
public class ASTParserLoadingTest {

	private final List<Long> createdAnimalIds = new ArrayList<>();

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	@Test
	public void testSubSelectAsArithmeticOperand(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "from Zoo z where ( select count(*) from Zoo ) = 0" ).list();

					// now as operands singly:
					s.createQuery( "from Zoo z where ( select count(*) from Zoo ) + 0 = 0" ).list();
					s.createQuery( "from Zoo z where 0 + ( select count(*) from Zoo ) = 0" ).list();

					// and doubly:
					s.createQuery( "from Zoo z where ( select count(*) from Zoo ) + ( select count(*) from Zoo ) = 0" )
							.list();
				}
		);
	}

	@Test
	@JiraKey("HHH-8432")
	public void testExpandListParameter(SessionFactoryScope scope) {
		final Object[] namesArray = new Object[] {
				"ZOO 1", "ZOO 2", "ZOO 3", "ZOO 4", "ZOO 5", "ZOO 6", "ZOO 7",
				"ZOO 8", "ZOO 9", "ZOO 10", "ZOO 11", "ZOO 12"
		};
		final Object[] citiesArray = new Object[] {
				"City 1", "City 2", "City 3", "City 4", "City 5", "City 6", "City 7",
				"City 8", "City 9", "City 10", "City 11", "City 12"
		};

		scope.inTransaction(
				(session) -> {
					Address address = new Address();
					Zoo zoo = new Zoo( "ZOO 1", address );
					address.setCity( "City 1" );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				(session) -> {
					List<Zoo> result = session.createQuery(
									"FROM Zoo z WHERE z.name IN (?1) and z.address.city IN (?2)", Zoo.class )
							.setParameterList( 1, namesArray )
							.setParameterList( 2, citiesArray )
							.list();
					assertThat( result.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-8699")
	public void testBooleanPredicate(SessionFactoryScope scope) {
		final Constructor created = scope.fromTransaction(
				(session) -> {
					final Constructor constructor = new Constructor();
					session.persist( constructor );
					return constructor;
				}
		);

		Constructor.resetConstructorExecutionCount();

		scope.inTransaction(
				(session) -> {
					final String qry = "select new Constructor( c.id, c.id is not null, c.id = c.id, c.id + 1, concat( str(c.id), 'foo' ) ) from Constructor c where c.id = :id";
					final Constructor result = session.createQuery( qry, Constructor.class )
							.setParameter( "id", created.getId() ).uniqueResult();
					assertThat( Constructor.getConstructorExecutionCount() ).isEqualTo( 1 );
					Constructor expected = new Constructor(
							created.getId(),
							true,
							true,
							created.getId() + 1,
							created.getId() + "foo"
					);
					assertThat( result ).isEqualTo( expected );
				}
		);
	}

	@Test
	public void testJpaTypeOperator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// where clause

					// control
					session.createQuery( "from Animal a where a.class = Dog", Animal.class ).list();
					// test
					session.createQuery( "from Animal a where type(a) = Dog", Animal.class ).list();


					// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					// select clause

					// control
					Query<?> query = session.createQuery( "select a.class from Animal a where a.class = Dog" );
					query.list();
					SqmSelectStatement<?> sqmStatement =
							(SqmSelectStatement<?>) query.unwrap( SqmQuery.class ).getSqmStatement();
					List<SqmSelection<?>> selections = sqmStatement.getQuerySpec().getSelectClause().getSelections();
					assertThat( selections.size() ).isEqualTo( 1 );
					SqmSelection<?> typeSelection = selections.get( 0 );
					// always integer for joined
					assertThat( typeSelection.getNodeJavaType().getJavaTypeClass() ).isEqualTo( Class.class );

					// test
					query = session.createQuery( "select type(a) from Animal a where type(a) = Dog" );
					query.list();
					sqmStatement = (SqmSelectStatement<?>) query.unwrap( SqmQuery.class ).getSqmStatement();
					selections = sqmStatement.getQuerySpec().getSelectClause().getSelections();
					assertThat( selections.size() ).isEqualTo( 1 );
					typeSelection = selections.get( 0 );
					assertThat( typeSelection.getNodeJavaType().getJavaTypeClass() ).isEqualTo( Class.class );
				}
		);
	}

	@Test
	public void testComponentJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					ComponentContainer root = new ComponentContainer(
							new ComponentContainer.Address(
									"123 Main",
									"Anywhere",
									"USA",
									new ComponentContainer.Address.Zip( 12345, 6789 )
							)
					);
					s.persist( root );
				}
		);

		scope.inTransaction(
				(s) -> {
					List result = s.createQuery( "select a from ComponentContainer c join c.address a" ).list();
					assertThat( result.size() ).isEqualTo( 1 );
					assertThat( result.get( 0 ) ).isInstanceOf( ComponentContainer.Address.class );

					result = s.createQuery( "select a.zip from ComponentContainer c join c.address a" ).list();
					assertThat( result.size() ).isEqualTo( 1 );
					assertThat( result.get( 0 ) ).isInstanceOf( ComponentContainer.Address.Zip.class );

					result = s.createQuery( "select z from ComponentContainer c join c.address a join a.zip z" ).list();
					assertThat( result.size() ).isEqualTo( 1 );
					assertThat( result.get( 0 ) ).isInstanceOf( ComponentContainer.Address.Zip.class );

					result = s.createQuery( "select z.code from ComponentContainer c join c.address a join a.zip z" )
							.list();
					assertThat( result.size() ).isEqualTo( 1 );
					assertThat( result.get( 0 ) ).isInstanceOf( Integer.class );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9642")
	public void testLazyAssociationInComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Address address = new Address();
					Zoo zoo = new Zoo( "ZOO 1", address );
					address.setCity( "City 1" );
					StateProvince stateProvince = new StateProvince();
					stateProvince.setName( "Illinois" );
					session.persist( stateProvince );
					address.setStateProvince( stateProvince );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Zoo zoo = session.createQuery( "from Zoo z", Zoo.class ).uniqueResult();
					assertThat( zoo ).isNotNull();
					assertThat( zoo.getAddress() ).isNotNull();
					assertThat( zoo.getAddress().getCity() ).isEqualTo( "City 1" );
					assertThat( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) ).isFalse();
					assertThat( zoo.getAddress().getStateProvince().getName() ).isEqualTo( "Illinois" );
					assertThat( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) ).isTrue();
				}
		);


		scope.inTransaction(
				(session) -> {
					final Zoo zoo = session.createQuery( "from Zoo z join fetch z.address.stateProvince", Zoo.class )
							.uniqueResult();
					assertThat( zoo ).isNotNull();
					assertThat( zoo.getAddress() ).isNotNull();
					assertThat( zoo.getAddress().getCity() ).isEqualTo( "City 1" );
					assertThat( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) ).isTrue();
					assertThat( zoo.getAddress().getStateProvince().getName() ).isEqualTo( "Illinois" );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Zoo zoo = session.createQuery( "from Zoo z join fetch z.address a join fetch a.stateProvince",
							Zoo.class ).uniqueResult();
					assertThat( zoo ).isNotNull();
					assertThat( zoo.getAddress() ).isNotNull();
					assertThat( zoo.getAddress().getCity() ).isEqualTo( "City 1" );
					assertThat( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) ).isTrue();
					assertThat( zoo.getAddress().getStateProvince().getName() ).isEqualTo( "Illinois" );
				}
		);
	}

	@Test
	public void testJPAQLQualifiedIdentificationVariablesControl(SessionFactoryScope scope) {
		// just checking syntax here...
		scope.inTransaction(
				session -> {
					session.createQuery( "from VariousKeywordPropertyEntity where type = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity where value = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity where key = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity where entry = 'something'" ).list();

					session.createQuery( "from VariousKeywordPropertyEntity e where e.type = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity e where e.value = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity e where e.key = 'something'" ).list();
					session.createQuery( "from VariousKeywordPropertyEntity e where e.entry = 'something'" ).list();
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJPAQLMapKeyQualifier(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human me = new Human();
					me.setName( new Name( "Steve", null, "Ebersole" ) );
					Human joe = new Human();
					me.setName( new Name( "Joe", null, "Ebersole" ) );
					me.setFamily( new HashMap() );
					me.getFamily().put( "son", joe );
					session.persist( me );
					session.persist( joe );
				}
		);

		// in SELECT clause

		scope.inTransaction(
				session -> {
					// hibernate-only form
					List results = session.createQuery( "select distinct key(h.family) from Human h" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object key = results.get( 0 );
					assertThat( String.class.isAssignableFrom( key.getClass() ) ).isTrue();
				}
		);

		scope.inTransaction(
				session -> {
					// jpa form
					List results = session.createQuery( "select distinct KEY(f) from Human h join h.family f" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object key = results.get( 0 );
					assertThat( String.class.isAssignableFrom( key.getClass() ) ).isTrue();
				}
		);

		// in WHERE clause
		scope.inTransaction(
				session -> {
					// hibernate-only form

					Long count = session.createQuery( "select count(*) from Human h where KEY(h.family) = 'son'",
									Long.class )
							.uniqueResult();
					assertThat( count ).isEqualTo( 1L );
				}
		);

		scope.inTransaction(
				session -> {
					// jpa form
					Long count = session.createQuery(
									"select count(*) from Human h join h.family f where key(f) = 'son'", Long.class )
							.uniqueResult();
					assertThat( count ).isEqualTo( 1L );
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJPAQLMapEntryQualifier(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human me = new Human();
					me.setName( new Name( "Steve", null, "Ebersole" ) );
					Human joe = new Human();
					me.setName( new Name( "Joe", null, "Ebersole" ) );
					me.setFamily( new HashMap() );
					me.getFamily().put( "son", joe );
					session.persist( me );
					session.persist( joe );
				}
		);

		// in SELECT clause
		scope.inTransaction(
				session -> {
					// hibernate-only form
					List results = session.createQuery( "select entry(h.family) from Human h" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object result = results.get( 0 );
					assertThat( Map.Entry.class.isAssignableFrom( result.getClass() ) ).isTrue();
					Map.Entry entry = (Map.Entry) result;
					assertThat( String.class.isAssignableFrom( entry.getKey().getClass() ) ).isTrue();
					assertThat( Human.class.isAssignableFrom( entry.getValue().getClass() ) ).isTrue();

				}
		);

		scope.inTransaction(
				session -> {
					// jpa form
					List results = session.createQuery( "select ENTRY(f) from Human h join h.family f" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object result = results.get( 0 );
					assertThat( Map.Entry.class.isAssignableFrom( result.getClass() ) ).isTrue();
					Map.Entry entry = (Map.Entry) result;
					assertThat( String.class.isAssignableFrom( entry.getKey().getClass() ) ).isTrue();
					assertThat( Human.class.isAssignableFrom( entry.getValue().getClass() ) ).isTrue();
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJPAQLMapValueQualifier(SessionFactoryScope scope) {
		Human joe = new Human();
		scope.inTransaction(
				session -> {
					Human me = new Human();
					me.setName( new Name( "Steve", null, "Ebersole" ) );
					me.setName( new Name( "Joe", null, "Ebersole" ) );
					me.setFamily( new HashMap() );
					me.getFamily().put( "son", joe );
					session.persist( me );
					session.persist( joe );
				}
		);

		// in SELECT clause
		scope.inTransaction(
				session -> {
					// hibernate-only form
					List results = session.createQuery( "select value(h.family) from Human h" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object result = results.get( 0 );
					assertThat( Human.class.isAssignableFrom( result.getClass() ) ).isTrue();
				}
		);

		scope.inTransaction(
				session -> {
					// jpa form
					List results = session.createQuery( "select VALUE(f) from Human h join h.family f" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object result = results.get( 0 );
					assertThat( Human.class.isAssignableFrom( result.getClass() ) ).isTrue();
				}
		);

		// in WHERE clause
		scope.inTransaction(
				session -> {
					// hibernate-only form
					Long count = session.createQuery( "select count(*) from Human h where VALUE(h.family) = :joe",
							Long.class ).setParameter( "joe", joe ).uniqueResult();
					// ACTUALLY EXACTLY THE SAME AS:
					// select count(*) from Human h where h.family = :joe
					assertThat( count ).isEqualTo( 1L );
				}
		);

		scope.inTransaction(
				session -> {
					// jpa form
					Long count = session.createQuery(
									"select count(*) from Human h join h.family f where value(f) = :joe", Long.class )
							.setParameter( "joe", joe ).uniqueResult();
					// ACTUALLY EXACTLY THE SAME AS:
					// select count(*) from Human h join h.family f where f = :joe
					assertThat( count ).isEqualTo( 1L );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void testPaginationWithPolymorphicQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setName( new Name( "Steve", null, "Ebersole" ) );
					session.persist( h );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from java.lang.Object" ).setMaxResults( 2 ).list();
					assertThat( results.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2045")
	@RequiresDialect(H2Dialect.class)
	public void testEmptyInList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human();
					human.setName( new Name( "Lukasz", null, "Antoniak" ) );
					human.setNickName( "NONE" );
					session.persist( human );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Human h where h.nickName in ()" ).list();
					assertThat( results.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-8901")
	public void testEmptyInListForDialectsNotSupportsEmptyInList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human();
					human.setName( new Name( "Lukasz", null, "Antoniak" ) );
					human.setNickName( "NONE" );
					session.persist( human );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Human h where h.nickName in (:nickNames)" )
							.setParameter( "nickNames", Collections.emptySet() )
							.list();
					assertThat( results.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2851")
	public void testMultipleRefsToSameParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setName( new Name( "Johnny", 'B', "Goode" ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( "Steve", null, "Ebersole" ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( "Bono", null, null ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( "Steve", 'Z', "Johnny" ) );
					h.setIntValue( 1 );
					session.persist( h );
					h = new Human();
					h.setName( new Name( null, null, null ) );
					session.persist( h );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Human where name.first = :name or name.last=:name" )
							.setParameter( "name", "Johnny" )
							.list();
					assertThat( results.size() ).isEqualTo( 2 );

					results = session.createQuery( "from Human where name.last = :name or :name is null" )
							.setParameter( "name", "Goode" )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					results = session.createQuery( "from Human where :name is null or name.last = :name" )
							.setParameter( "name", "Goode" )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );

					results = session.createQuery(
									"from Human where name.first = :firstName and (name.last = :name or :name is null)" )
							.setParameter( "firstName", "Bono" )
							.setParameter( "name", null )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					results = session.createQuery(
									"from Human where name.first = :firstName and ( :name is null  or name.last = cast(:name as string) )" )
							.setParameter( "firstName", "Bono" )
							.setParameter( "name", null )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );

					results = session.createQuery( "from Human where intValue = :intVal or :intVal is null" )
							.setParameter( "intVal", 1 )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					results = session.createQuery( "from Human where :intVal is null or intValue = :intVal" )
							.setParameter( "intVal", 1 )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );


					results = session.createQuery( "from Human where intValue = :intVal or :intVal is null" )
							.setParameter( "intVal", null )
							.list();
					assertThat( results.size() ).isEqualTo( 5 );
					results = session.createQuery( "from Human where :intVal is null or intValue is null" )
							.setParameter( "intVal", null )
							.list();
					assertThat( results.size() ).isEqualTo( 5 );
				}
		);
	}

	@Test
	public void testComponentNullnessChecks(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setName( new Name( "Johnny", 'B', "Goode" ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( "Steve", null, "Ebersole" ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( "Bono", null, null ) );
					session.persist( h );
					h = new Human();
					h.setName( new Name( null, null, null ) );
					session.persist( h );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Human where name is null" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					results = session.createQuery( "from Human where name is not null" ).list();
					assertThat( results.size() ).isEqualTo( 3 );
					Dialect dialect = session.getDialect();
					String query =
							(dialect instanceof DB2Dialect || dialect instanceof HSQLDialect) ?
									"from Human where cast(?1 as string) is null" :
									"from Human where ?1 is null";
					if ( dialect instanceof DerbyDialect ) {
						session.createQuery( query ).setParameter( 1, "null" ).list();
					}
					else {
						session.createQuery( query ).setParameter( 1, null ).list();
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4150")
	public void testSelectClauseCaseWithSum(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h1 = new Human();
					h1.setBodyWeight( 74.0f );
					h1.setDescription( "Me" );
					session.persist( h1 );

					Human h2 = new Human();
					h2.setBodyWeight( 125.0f );
					h2.setDescription( "big persion #1" );
					session.persist( h2 );

					Human h3 = new Human();
					h3.setBodyWeight( 110.0f );
					h3.setDescription( "big persion #2" );
					session.persist( h3 );

					session.flush();

					Number count = (Number) session.createQuery(
							"select sum(case when bodyWeight > 100 then 1 else 0 end) from Human" ).uniqueResult();
					assertThat( count.intValue() ).isEqualTo( 2 );
					count = (Number) session.createQuery(
									"select sum(case when bodyWeight > 100 then bodyWeight else 0 end) from Human" )
							.uniqueResult();
					assertThat( count.floatValue() ).isEqualTo( h2.getBodyWeight() + h3.getBodyWeight(),
							Offset.offset( 0.001F ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4150")
	@SkipForDialect( dialectClass = InformixDialect.class, majorVersion = 11, minorVersion = 70, reason = "Informix does not support case with count distinct")
	public void testSelectClauseCaseWithCountDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h1 = new Human();
					h1.setBodyWeight( 74.0f );
					h1.setDescription( "Me" );
					h1.setNickName( "Oney" );
					session.persist( h1 );

					Human h2 = new Human();
					h2.setBodyWeight( 125.0f );
					h2.setDescription( "big persion" );
					h2.setNickName( "big #1" );
					session.persist( h2 );

					Human h3 = new Human();
					h3.setBodyWeight( 110.0f );
					h3.setDescription( "big persion" );
					h3.setNickName( "big #2" );
					session.persist( h3 );

					session.flush();

					Number count = (Number) session.createQuery(
									"select count(distinct case when bodyWeight > 100 then description else null end) from Human" )
							.uniqueResult();
					assertThat( count.intValue() ).isEqualTo( 1 );
					count = (Number) session.createQuery(
									"select count(case when bodyWeight > 100 then description else null end) from Human" )
							.uniqueResult();
					assertThat( count.intValue() ).isEqualTo( 2 );
					count = (Number) session.createQuery(
									"select count(distinct case when bodyWeight > 100 then nickName else null end) from Human" )
							.uniqueResult();
					assertThat( count.intValue() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testInvalidCollectionDereferencesFail(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					// control group...
					scope.inTransaction(
							s,
							session -> {
								s.createQuery( "from Animal a join a.offspring o where o.description = 'xyz'",
												Object[].class )
										.list();
								s.createQuery( "from Animal a join a.offspring o where o.father.description = 'xyz'",
										Object[].class ).list();
								s.createQuery( "from Animal a join a.offspring o order by o.description",
												Object[].class )
										.list();
								s.createQuery( "from Animal a join a.offspring o order by o.father.description",
										Object[].class ).list();
							}
					);

					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "from Animal a where a.offspring.description = 'xyz'" ).list();
									fail( "illegal collection dereference semantic did not cause failure" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException qe) {
									//expected
								}
							}
					);

					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "from Animal a where a.offspring.father.description = 'xyz'" )
											.list();
									fail( "illegal collection dereference semantic did not cause failure" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException qe) {
									//expected
								}
							}
					);

					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "from Animal a order by a.offspring.description" ).list();
									fail( "illegal collection dereference semantic did not cause failure" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException qe) {
									//expected
								}
							}
					);

					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "from Animal a order by a.offspring.father.description" ).list();
									fail( "illegal collection dereference semantic did not cause failure" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException qe) {
									//expected
								}
							}
					);
				}
		);
	}

	@Test
	public void testConcatenation(SessionFactoryScope scope) {
		// simple syntax checking...
		scope.inTransaction(
				session ->
						session.createQuery( "from Human h where h.nickName = '1' || 'ov' || 'tha' || 'few'" ).list()
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testExpressionWithParamInFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Dialect dialect = session.getDialect();
					session.createQuery( "from Animal a where abs(a.bodyWeight-:param) < 2.0" )
							.setParameter( "param", 1 ).list();
					session.createQuery( "from Animal a where abs(:param - a.bodyWeight) < 2.0" )
							.setParameter( "param", 1 ).list();
					if ( dialect instanceof HSQLDialect || dialect instanceof DB2Dialect || dialect instanceof DerbyDialect ) {
						// HSQLDB and DB2 don't like the abs(? - ?) syntax. bit work if at least one parameter is typed...
						session.createQuery( "from Animal where abs(cast(:x as long) - :y) < 2.0" )
								.setParameter( "x", 1 )
								.setParameter( "y", 1 ).list();
						session.createQuery( "from Animal where abs(:x - cast(:y as long)) < 2.0" )
								.setParameter( "x", 1 )
								.setParameter( "y", 1 ).list();
						session.createQuery( "from Animal where abs(cast(:x as long) - cast(:y as long)) < 2.0" )
								.setParameter( "x", 1 )
								.setParameter( "y", 1 ).list();
					}
					else {
						session.createQuery( "from Animal where abs(:x - :y) < 2.0" ).setParameter( "x", 1 )
								.setParameter( "y", 1 )
								.list();
					}

					if ( dialect instanceof DB2Dialect ) {
						session.createQuery( "from Animal where lower(upper(cast(:foo as string))) like 'f%'" )
								.setParameter( "foo", "foo" ).list();
					}
					else {
						session.createQuery( "from Animal where lower(upper(:foo)) like 'f%'" )
								.setParameter( "foo", "foo" ).list();
					}

					session.createQuery(
									"from Animal a where abs(abs(a.bodyWeight - 1.0 + :param) * abs(length('ffobar')-3)) = 3.0" )
							.setParameter( "param", 1 ).list();

					if ( dialect instanceof DB2Dialect ) {
						session.createQuery(
										"from Animal where lower(upper('foo') || upper(cast(:bar as string))) like 'f%'" )
								.setParameter( "bar", "xyz" ).list();
					}
					else {
						session.createQuery( "from Animal where lower(upper('foo') || upper(:bar)) like 'f%'" )
								.setParameter( "bar", "xyz" ).list();
					}

					if ( dialect instanceof HANADialect ) {
						session.createQuery( "from Animal where abs(cast(1 as double) - cast(:param as double)) = 1.0" )
								.setParameter( "param", 1 ).list();
					}
					else if ( !(dialect instanceof PostgreSQLDialect || dialect instanceof MySQLDialect) ) {
						session.createQuery( "from Animal where abs(cast(1 as float) - cast(:param as float)) = 1.0" )
								.setParameter( "param", 1 ).list();
					}
				}
		);
	}

	@Test
	public void testCrazyIdFieldNames(SessionFactoryScope scope) {
		MoreCrazyIdFieldNameStuffEntity top = new MoreCrazyIdFieldNameStuffEntity( "top" );
		HeresAnotherCrazyIdFieldName next = new HeresAnotherCrazyIdFieldName( "next" );
		top.setHeresAnotherCrazyIdFieldName( next );
		MoreCrazyIdFieldNameStuffEntity other = new MoreCrazyIdFieldNameStuffEntity( "other" );

		scope.inTransaction(
				session -> {
					session.persist( next );
					session.persist( top );
					session.persist( other );
					session.flush();

					List results = session.createQuery(
									"select e.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e where e.heresAnotherCrazyIdFieldName is not null" )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					Object result = results.get( 0 );
					assertClassAssignability( HeresAnotherCrazyIdFieldName.class, result.getClass() );
					assertThat( result ).isSameAs( next );

					results = session.createQuery(
									"select e.heresAnotherCrazyIdFieldName.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e where e.heresAnotherCrazyIdFieldName is not null" )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					result = results.get( 0 );
					assertClassAssignability( Long.class, result.getClass() );
					assertThat( result ).isEqualTo( next.getHeresAnotherCrazyIdFieldName() );

					results = session.createQuery(
									"select e.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e" )
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2257")
	public void testImplicitJoinsInDifferentClauses(SessionFactoryScope scope) {
		// both the classic and ast translators output the same syntactically valid sql
		// for all of these cases; the issue is that shallow (iterate) and
		// non-shallow (list/scroll) queries return different results because the
		// shallow skips the inner join which "weeds out" results from the non-shallow queries.
		// The results were initially different depending upon the clause(s) in which the
		// implicit join occurred
		scope.inTransaction(
				session -> {
					SimpleEntityWithAssociation owner = new SimpleEntityWithAssociation( "owner" );
					SimpleAssociatedEntity e1 = new SimpleAssociatedEntity( "thing one", owner );
					SimpleAssociatedEntity e2 = new SimpleAssociatedEntity( "thing two" );
					session.persist( e1 );
					session.persist( e2 );
					session.persist( owner );
				}
		);

		checkCounts(
				"select e.owner from SimpleAssociatedEntity e",
				1,
				"implicit-join in select clause",
				scope );
		checkCounts(
				"select e.id, e.owner from SimpleAssociatedEntity e",
				1,
				"implicit-join in select clause",
				scope );

		// resolved to a "id short cut" when part of the order by clause -> no inner join = no weeding out...
		checkCounts(
				"from SimpleAssociatedEntity e order by e.owner",
				2,
				"implicit-join in order-by clause",
				scope );
		// resolved to a "id short cut" when part of the group by clause -> no inner join = no weeding out...
		checkCounts(
				"select e.owner.id, count(*) from SimpleAssociatedEntity e group by e.owner",
				2,
				"implicit-join in select and group-by clauses",
				scope
		);
	}

	@Test
	public void testRowValueConstructorSyntaxInInList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product = new Product();
					product.setDescription( "My Product" );
					product.setNumberAvailable( 10 );
					product.setPrice( new BigDecimal( 123 ) );
					product.setProductId( "4321" );
					session.persist( product );

					Customer customer = new Customer();
					customer.setCustomerId( "123456789" );
					customer.setName( "My customer" );
					customer.setAddress( "somewhere" );
					session.persist( customer );

					Order order = customer.generateNewOrder( new BigDecimal( 1234 ) );
					session.persist( order );

					LineItem li = order.generateLineItem( product, 5 );
					session.persist( li );
					product = new Product();
					product.setDescription( "My Product" );
					product.setNumberAvailable( 10 );
					product.setPrice( new BigDecimal( 123 ) );
					product.setProductId( "1234" );
					session.persist( product );
					li = order.generateLineItem( product, 10 );
					session.persist( li );

					session.flush();

					Query query = session.createQuery( "from LineItem l where l.id in (:idList)" );
					List<Id> list = new ArrayList<Id>();
					list.add( new Id( "123456789", order.getId().getOrderNumber(), "4321" ) );
					list.add( new Id( "123456789", order.getId().getOrderNumber(), "1234" ) );
					query.setParameterList( "idList", list );
					assertThat( query.list().size() ).isEqualTo( 2 );

					query = session.createQuery( "from LineItem l where l.id in :idList" );
					query.setParameterList( "idList", list );
					assertThat( query.list().size() ).isEqualTo( 2 );
				}
		);
	}

	private void checkCounts(String hql, int expected, String testCondition, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int count = determineCount( session.createQuery( hql ).list().iterator() );
					assertThat( count ).as( "list() [" + testCondition + "]" ).isEqualTo( expected );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2257")
	public void testImplicitSelectEntityAssociationInShallowQuery(SessionFactoryScope scope) {
		// both the classic and ast translators output the same syntactically valid sql.
		// the issue is that shallow and non-shallow queries return different
		// results because the shallow skips the inner join which "weeds out" results
		// from the non-shallow queries...
		scope.inTransaction(
				session -> {
					SimpleEntityWithAssociation owner = new SimpleEntityWithAssociation( "owner" );
					SimpleAssociatedEntity e1 = new SimpleAssociatedEntity( "thing one", owner );
					SimpleAssociatedEntity e2 = new SimpleAssociatedEntity( "thing two" );
					session.persist( e1 );
					session.persist( e2 );
					session.persist( owner );
				}
		);

		scope.inTransaction(
				session -> {
					int count = determineCount(
							session.createQuery( "select e.id, e.owner from SimpleAssociatedEntity e" ).list()
									.iterator() );
					// thing two would be removed from the result due to the inner join
					assertThat( count ).isEqualTo( 1 );
				}
		);
	}

	private int determineCount(Iterator iterator) {
		int count = 0;
		while ( iterator.hasNext() ) {
			count++;
			iterator.next();
		}
		return count;
	}

	@Test
	@JiraKey(value = "HHH-6714")
	public void testUnaryMinus(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human stliu = new Human();
					stliu.setIntValue( 26 );

					session.persist( stliu );
				}
		);

		scope.inTransaction(
				session -> {
					List list = session.createQuery( "from Human h where -(h.intValue - 100)=74" ).list();
					assertThat( list.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testEntityAndOneToOneReturnedByQuery(SessionFactoryScope scope) {
		Human h = new Human();
		User u = new User();
		scope.inTransaction(
				session -> {
					h.setName( new Name( "Gail", null, "Badner" ) );
					session.persist( h );
					u.setUserName( "gbadner" );
					u.setHuman( h );
					session.persist( u );
				}
		);

		scope.inTransaction(
				session -> {
					Object[] result = session.createQuery( "from User u, Human h where u.human = h", Object[].class )
							.uniqueResult();
					assertThat( result ).isNotNull();
					assertThat( ((User) result[0]).getUserName() ).isEqualTo( u.getUserName() );
					assertThat( ((Human) result[1]).getName().getFirst() ).isEqualTo( h.getName().getFirst() );
					assertThat( result[1] ).isSameAs( ((User) result[0]).getHuman() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9305")
	public void testExplicitToOneInnerJoin(SessionFactoryScope scope) {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		scope.inTransaction(
				session -> {
					session.persist( title1 );
					session.persist( dept1 );
					session.persist( employee1 );
					session.persist( title2 );
					session.persist( employee2 );
				}
		);

		scope.inTransaction(
				session -> {
					Department department = session.createQuery(
									"select e.department from Employee e inner join e.department", Department.class )
							.uniqueResult();
					assertThat( department.getDeptName() ).isEqualTo( employee1.getDepartment().getDeptName() );

				}
		);
	}

	@Test
	public void testExplicitToOneOuterJoin(SessionFactoryScope scope) {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		scope.inTransaction(
				session -> {
					session.persist( title1 );
					session.persist( dept1 );
					session.persist( employee1 );
					session.persist( title2 );
					session.persist( employee2 );
				}
		);

		scope.inTransaction(
				session -> {
					List list = session.createQuery( "select e.department from Employee e left join e.department" )
							.list();
					assertThat( list.size() ).isEqualTo( 2 );
					final Department dept;
					if ( list.get( 0 ) == null ) {
						dept = (Department) list.get( 1 );
					}
					else {
						dept = (Department) list.get( 0 );
						assertThat( list.get( 1 ) ).isNull();
					}
					assertThat( dept.getDeptName() ).isEqualTo( dept1.getDeptName() );
				}
		);
	}

	@Test
	public void testExplicitToOneInnerJoinAndImplicitToOne(SessionFactoryScope scope) {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		scope.inTransaction(
				session -> {
					session.persist( title1 );
					session.persist( dept1 );
					session.persist( employee1 );
					session.persist( title2 );
					session.persist( employee2 );
				}
		);

		scope.inTransaction(
				session -> {
					Object[] result = (Object[]) session.createQuery(
							"select e.firstName, e.lastName, e.title.description, e.department from Employee e inner join e.department"
					).uniqueResult();
					assertThat( result[0] ).isEqualTo( employee1.getFirstName() );
					assertThat( result[1] ).isEqualTo( employee1.getLastName() );
					assertThat( result[2] ).isEqualTo( employee1.getTitle().getDescription() );
					assertThat( ((Department) result[3]).getDeptName() ).isEqualTo(
							employee1.getDepartment().getDeptName() );
				}
		);
	}

	@Test
	public void testNestedComponentIsNull(SessionFactoryScope scope) {
		// (1) From MapTest originally...
		// (2) Was then moved into HQLTest...
		// (3) However, a bug fix to EntityType#getIdentifierOrUniqueKeyType (HHH-2138)
		// 		caused the classic parser to suddenly start throwing exceptions on
		//		this query, apparently relying on the buggy behavior somehow; thus
		//		moved here to at least get some syntax checking...
		//
		// fyi... found and fixed the problem in the classic parser; still
		// leaving here for syntax checking
		new SyntaxChecker( "from Commento c where c.marelo.commento.mcompr is null" ).checkAll( scope );
	}

	@Test
	@JiraKey(value = "HHH-939")
	public void testSpecialClassPropertyReference(SessionFactoryScope scope) {
		// this is a long standing bug in Hibernate when applied to joined-subclasses;
		//  see HHH-939 for details and history
		new SyntaxChecker( "from Zoo zoo where zoo.class = PettingZoo" ).checkAll( scope );
		new SyntaxChecker( "select a.description from Animal a where a.class = Mammal" ).checkAll( scope );
		new SyntaxChecker( "select a.class from Animal a" ).checkAll( scope );
		new SyntaxChecker( "from DomesticAnimal an where an.class = Dog" ).checkAll( scope );
		new SyntaxChecker( "from Animal an where an.class = Dog" ).checkAll( scope );
	}

	@Test
	@JiraKey(value = "HHH-2376")
	public void testSpecialClassPropertyReferenceFQN(SessionFactoryScope scope) {
		new SyntaxChecker( "from Zoo zoo where zoo.class = org.hibernate.orm.test.hql.PettingZoo" ).checkAll( scope );
		new SyntaxChecker(
				"select a.description from Animal a where a.class = org.hibernate.orm.test.hql.Mammal" ).checkAll(
				scope );
		new SyntaxChecker( "from DomesticAnimal an where an.class = org.hibernate.orm.test.hql.Dog" ).checkAll( scope );
		new SyntaxChecker( "from Animal an where an.class = org.hibernate.orm.test.hql.Dog" ).checkAll( scope );
	}

	@Test
	@JiraKey(value = "HHH-1631")
	public void testSubclassOrSuperclassPropertyReferenceInJoinedSubclass(SessionFactoryScope scope) {
		// this is a long standing bug in Hibernate; see HHH-1631 for details and history
		//
		// (1) pregnant is defined as a property of the class (Mammal) itself
		// (2) description is defined as a property of the superclass (Animal)
		// (3) name is defined as a property of a particular subclass (Human)

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkAll( scope );

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.pregnant = false" ).checkAll( scope );
		new SyntaxChecker( "select m.pregnant from Zoo z join z.mammals as m where m.pregnant = false" ).checkAll(
				scope );

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.description = 'tabby'" ).checkAll( scope );
		new SyntaxChecker(
				"select m.description from Zoo z join z.mammals as m where m.description = 'tabby'" ).checkAll( scope );

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkAll( scope );
		new SyntaxChecker( "select m.name from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkAll(
				scope );

		new SyntaxChecker( "select m.pregnant from Zoo z join z.mammals as m" ).checkAll( scope );
		new SyntaxChecker( "select m.description from Zoo z join z.mammals as m" ).checkAll( scope );
		new SyntaxChecker( "select m.name from Zoo z join z.mammals as m" ).checkAll( scope );

		new SyntaxChecker( "from DomesticAnimal da join da.owner as o where o.nickName = 'Gavin'" ).checkAll( scope );
		new SyntaxChecker(
				"select da.father from DomesticAnimal da join da.owner as o where o.nickName = 'Gavin'" ).checkAll(
				scope );
		new SyntaxChecker( "select da.father from DomesticAnimal da where da.owner.nickName = 'Gavin'" ).checkAll(
				scope );
	}

	/**
	 * {@link #testSubclassOrSuperclassPropertyReferenceInJoinedSubclass} tests the implicit form of entity casting
	 * that Hibernate has always supported.  THis method tests the explicit variety added by JPA 2.1 using the TREAT
	 * keyword.
	 */
	@Test
	public void testExplicitEntityCasting(SessionFactoryScope scope) {
		new SyntaxChecker( "from Zoo z join treat(z.mammals as Human) as m where m.name.first = 'John'" ).checkAll(
				scope );
		new SyntaxChecker( "from Zoo z join z.mammals as m where treat(m as Human).name.first = 'John'" ).checkAll(
				scope );
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportLimitAndOffsetCheck.class,
			comment = "dialect does not support offset and limit combo"
	)
	public void testSimpleSelectWithLimitAndOffset(SessionFactoryScope scope) throws Exception {
		// just checking correctness of param binding code...
		scope.inTransaction(
				session ->
						session.createQuery( "from Animal" )
								.setFirstResult( 2 )
								.setMaxResults( 1 )
								.list()
		);
	}

	@Test
	public void testJPAPositionalParameterList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ArrayList<String> params = new ArrayList<>();
					params.add( "Doe" );
					params.add( "Public" );
					session.createQuery( "from Human where name.last in (?1)" )
							.setParameterList( 1, params )
							.list();

					session.createQuery( "from Human where name.last in ?1" )
							.setParameterList( 1, params )
							.list();

					session.createQuery( "from Human where nickName = ?1 and ( name.first = ?2 or name.last in (?3) )" )
							.setParameter( 1, "Yogster" )
							.setParameter( 2, "Yogi" )
							.setParameterList( 3, params )
							.list();

					session.createQuery( "from Human where nickName = ?1 and ( name.first = ?2 or name.last in ?3 )" )
							.setParameter( 1, "Yogster" )
							.setParameter( 2, "Yogi" )
							.setParameterList( 3, params )
							.list();

					session.createQuery( "from Human where nickName = ?1 or ( name.first = ?2 and name.last in (?3) )" )
							.setParameter( 1, "Yogster" )
							.setParameter( 2, "Yogi" )
							.setParameterList( 3, params )
							.list();

					session.createQuery( "from Human where nickName = ?1 or ( name.first = ?2 and name.last in ?3 )" )
							.setParameter( 1, "Yogster" )
							.setParameter( 2, "Yogi" )
							.setParameterList( 3, params )
							.list();
				}
		);
	}

	@Test
	public void testComponentQueries(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<?> query = session.createQuery( "select h.name from Human h" );
					final SqmSelectStatement<?> sqmStatement =
							(SqmSelectStatement<?>) query.unwrap( SqmQuery.class ).getSqmStatement();
					assertThat( sqmStatement.getQuerySpec().getSelectClause().getSelections().size() ).isEqualTo( 1 );
					final SqmSelection<?> selection = sqmStatement.getQuerySpec().getSelectClause().getSelections()
							.get( 0 );
					final SqmExpressible<?> selectionType = selection.getSelectableNode().getNodeType();
					assertThat( selectionType ).isInstanceOf( EmbeddableDomainType.class );
					assertThat( selection.getNodeJavaType().getJavaTypeClass() ).isEqualTo( Name.class );


					// Test the ability to perform comparisons between component values
					session.createQuery( "from Human h where h.name = h.name" ).list();
					session.createQuery( "from Human h where h.name = :name" ).setParameter( "name", new Name() )
							.list();
					session.createQuery( "from Human where name = :name" ).setParameter( "name", new Name() ).list();
					session.createQuery( "from Human h where :name = h.name" ).setParameter( "name", new Name() )
							.list();
					session.createQuery( "from Human h where :name <> h.name" ).setParameter( "name", new Name() )
							.list();

					// Test the ability to perform comparisons between a component and an explicit row-value
					session.createQuery( "from Human h where h.name = ('John', 'X', 'Doe')" ).list();
					session.createQuery( "from Human h where ('John', 'X', 'Doe') = h.name" ).list();
					session.createQuery( "from Human h where ('John', 'X', 'Doe') <> h.name" ).list();

					session.createQuery( "from Human h order by h.name" ).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1774")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void testComponentParameterBinding(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order.Id oId = new Order.Id( "1234", 1 );

					// control
					session.createQuery( "from Order o where o.customer.name =:name and o.id = :id" )
							.setParameter( "name", "oracle" )
							.setParameter( "id", oId )
							.list();

					// this is the form that caused problems in the original case...
					session.createQuery( "from Order o where o.id = :id and o.customer.name =:name " )
							.setParameter( "id", oId )
							.setParameter( "name", "oracle" )
							.list();
				}
		);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testAnyMappingReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PropertyValue redValue = new StringPropertyValue( "red" );
					PropertyValue loneliestNumberValue = new IntegerPropertyValue( 1 );

					Long id;
					PropertySet ps = new PropertySet( "my properties" );
					ps.setSomeSpecificProperty( redValue );
					ps.getGeneralProperties().put( "the loneliest number", loneliestNumberValue );
					ps.getGeneralProperties().put( "i like", new StringPropertyValue( "pina coladas" ) );
					ps.getGeneralProperties()
							.put( "i also like", new StringPropertyValue( "getting caught in the rain" ) );
					session.persist( ps );

					session.getTransaction().commit();
					id = ps.getId();
					session.clear();
					session.beginTransaction();

					// TODO : setEntity() currently will not work here, but that would be *very* nice
					// does not work because the corresponding EntityType is then used as the "bind type" rather
					// than the "discovered" AnyType...
					session.createQuery( "from PropertySet p where p.someSpecificProperty = :ssp", PropertySet.class )
							.setParameter( "ssp", redValue ).list();

					session.createQuery( "from PropertySet p where p.someSpecificProperty.id is not null",
							PropertySet.class ).list();

					session.createQuery( "from PropertySet p join p.generalProperties gp where gp.id is not null",
									PropertySet.class )
							.list();
				}
		);
	}

	@Test
	public void testJdkEnumStyleEnumConstant(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session ->
						session.createQuery(
										"from Zoo z where z.classification = org.hibernate.orm.test.hql.Classification.LAME" )
								.list()

		);
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testParameterTypeMismatch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						session.createQuery( "from Animal a where a.description = :nonstring" )
								.setParameter( "nonstring", Integer.valueOf( 1 ) )
								.list();
						fail( "query execution should have failed" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( TypeMismatchException.class, e.getCause() );
					}
					catch (TypeMismatchException tme) {
						// expected behavior
					}
				}
		);
	}

	@Test
	public void testMultipleBagFetchesFail(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					scope.inTransaction(
							session -> {
								try {
									s.createQuery( "from Human h join fetch h.friends f join fetch f.friends fof" )
											.list();
									fail( "failure expected" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( MultipleBagFetchException.class, e.getCause() );
								}
								catch (HibernateException e) {
									assertThat( e.getMessage().indexOf( "multiple bags" ) > 0 ).isTrue();
								}
							}
					);
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1248")
	public void testCollectionJoinsInSubselect(SessionFactoryScope scope) {
		// HHH-1248 : initially FromElementFactory treated any explicit join
		// as an implied join so that theta-style joins would always be used.
		// This was because correlated subqueries cannot use ANSI-style joins
		// for the correlation.  However, this special treatment was not limited
		// to only correlated subqueries; it was applied to any subqueries ->
		// which in-and-of-itself is not necessarily bad.  But somewhere later
		// the choices made there caused joins to be dropped.
		scope.inTransaction(
				session -> {
					String qryString =
							"select a.id, a.description" +
							" from Animal a" +
							"       left join a.offspring" +
							" where a in (" +
							"       select a1 from Animal a1" +
							"           left join a1.offspring o" +
							"       where a1.id=1" +
							")";
					session.createQuery( qryString ).list();
					qryString =
							"select h.id, h.description" +
							" from Human h" +
							"      left join h.friends" +
							" where h in (" +
							"      select h1" +
							"      from Human h1" +
							"          left join h1.friends f" +
							"      where h1.id=1" +
							")";
					session.createQuery( qryString ).list();
					qryString =
							"select h.id, h.description" +
							" from Human h" +
							"      left join h.friends f" +
							" where f in (" +
							"      select h1" +
							"      from Human h1" +
							"          left join h1.friends f1" +
							"      where h = f1" +
							")";
					session.createQuery( qryString ).list();
				}
		);
	}

	@Test
	public void testCollectionFetchWithDistinctionAndLimit(SessionFactoryScope scope) {
		// create some test data...
		scope.inTransaction(
				session -> {
					int parentCount = 30;
					for ( int i = 0; i < parentCount; i++ ) {
						Animal child1 = new Animal();
						child1.setDescription( "collection fetch distinction (child1 - parent" + i + ")" );
						session.persist( child1 );
						Animal child2 = new Animal();
						child2.setDescription( "collection fetch distinction (child2 - parent " + i + ")" );
						session.persist( child2 );
						Animal parent = new Animal();
						parent.setDescription( "collection fetch distinction (parent" + i + ")" );
						parent.setSerialNumber( "123-" + i );
						parent.addOffspring( child1 );
						parent.addOffspring( child2 );
						session.persist( parent );
					}
				}
		);

		scope.inTransaction(
				session -> {
					// Test simple distinction
					List results;
					results = session.createQuery( "select distinct p from Animal p inner join fetch p.offspring" )
							.list();
					assertThat( results.size() ).as( "duplicate list() returns" ).isEqualTo( 30 );
					// Test first/max
					results = session.createQuery( "select p from Animal p inner join fetch p.offspring order by p.id" )
							.setFirstResult( 5 )
							.setMaxResults( 20 )
							.list();
					assertThat( results.size() ).as( "duplicate returns" ).isEqualTo( 20 );
					Animal firstReturn = (Animal) results.get( 0 );
					assertThat( firstReturn.getSerialNumber() ).as( "firstResult not applied correctly" )
							.isEqualTo( "123-5" );

				}
		);
	}

	@Test
	public void testFetchInSubqueryFails(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						session.createQuery(
										"from Animal a where a.mother in (select m from Animal a1 inner join a1.mother as m join fetch m.mother)" )
								.list();
						fail( "fetch join allowed in subquery" );
					}
					catch (IllegalArgumentException e) {
						assertTyping( QueryException.class, e.getCause() );
					}
					catch (QueryException expected) {
						// expected behavior
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1830")
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "Derby doesn't see that the subquery is functionally dependent")
	public void testAggregatedJoinAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select p.id, size( descendants ) " +
									"from Animal p " +
									"left outer join p.offspring descendants " +
									"group by p.id" )
							.list();
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-14533")
	@JiraKey(value = "HHH-14533")
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "Derby doesn't see that the subquery is functionally dependent")
	public void testAggregatedJoinAlias2(SessionFactoryScope scope) {
		Map<Long, Set> descendants = new HashMap<>();
		scope.inTransaction(
				session -> {
					// Add data
					// - assume IDs start with 1;
					// - integer in parentheses is value for #intValue.
					//
					// Generation 0:       1(5)   2(5)    4(5)            8(5)
					//                             |      /  \         /   |    \
					// Generation 1:              3(1)  5(1) 6(2)  9(1)  10(2)   12(3)
					//                                        |            |     |     \
					// Generation 2:                         7(5)        11(5)  13(5)  14(5)

					for ( int i = 1; i < 5; i++ ) {
						Human generation0 = new Human();
						generation0.setIntValue( 5 );
						session.persist( generation0 );
						generation0.setOffspring( new HashSet() );
						for ( int j = 1; j < i; j++ ) {
							Human generation1 = new Human();
							generation1.setMother( generation0 );
							generation0.getOffspring().add( generation1 );
							generation1.setOffspring( new HashSet() );
							generation1.setIntValue( j );
							session.persist( generation1 );
							for ( int k = 1; k < j; k++ ) {
								Human generation2 = new Human();
								generation2.setMother( generation1 );
								generation1.getOffspring().add( generation2 );
								generation2.setIntValue( 5 );
								session.persist( generation2 );
							}
							descendants.put( generation1.getId(), generation1.getOffspring());

						}
						descendants.put( generation0.getId(), generation0.getOffspring());
					}
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery(
									"select distinct p.id " +
									"from Animal p " +
									"left outer join p.offspring descendants " +
									"group by p.id" )
							.list();
					assertThat( results.size() ).isEqualTo( 14 );

					results = session.createQuery(
									"select p.id, size( descendants ) " +
									"from Animal p " +
									"left outer join p.offspring descendants " +
									"group by p.id order by p.id" )
							.list();

					assertThat( results.size() ).isEqualTo( 14 );
					assertThat( ((Object[]) results.get( 0 ))[1] ).isEqualTo( 0 ); // id 1
					assertThat( ((Object[]) results.get( 1 ))[1] ).isEqualTo( 1 ); // id 2
					assertThat( ((Object[]) results.get( 2 ))[1] ).isEqualTo( 0 ); // id 3
					assertThat( ((Object[]) results.get( 3 ))[1] ).isEqualTo( 2 ); // id 4
					assertThat( ((Object[]) results.get( 4 ))[1] ).isEqualTo( 0 ); // id 5
					assertThat( ((Object[]) results.get( 5 ))[1] ).isEqualTo( 1 ); // id 6
					assertThat( ((Object[]) results.get( 6 ))[1] ).isEqualTo( 0 ); // id 7
					assertThat( ((Object[]) results.get( 7 ))[1] ).isEqualTo( 3 ); // id 8
					assertThat( ((Object[]) results.get( 8 ))[1] ).isEqualTo( 0 ); // id 9
					assertThat( ((Object[]) results.get( 9 ))[1] ).isEqualTo( 1 ); // id 10
					assertThat( ((Object[]) results.get( 10 ))[1] ).isEqualTo( 0 ); // id 11
					assertThat( ((Object[]) results.get( 11 ))[1] ).isEqualTo( 2 ); // id 12
					assertThat( ((Object[]) results.get( 12 ))[1] ).isEqualTo( 0 ); // id 13
					assertThat( ((Object[]) results.get( 13 ))[1] ).isEqualTo( 0 ); // id 14

					// The following query results will depend on #intValue
					results = session.createQuery(
									"select p.id, size( descendants ) " +
									"from Animal p " +
									"left outer join p.offspring descendants " +
									"where descendants.intValue > 1 " +
									"group by p.id order by p.id" )
							.list();

					// Expect results for ids:        				4, 6, 8, 10, 12
					// Expected size(descendants.initValue > 1):    1, 1, 2,  1,  2
					assertThat( results.size() ).isEqualTo( 5 );
					assertThat( ((Object[]) results.get( 0 ))[1] ).isEqualTo( 1 );
					assertThat( ((Object[]) results.get( 1 ))[1] ).isEqualTo( 1 );
					assertThat( ((Object[]) results.get( 2 ))[1] ).isEqualTo( 2 );
					assertThat( ((Object[]) results.get( 3 ))[1] ).isEqualTo( 1 );
					assertThat( ((Object[]) results.get( 4 ))[1] ).isEqualTo( 2 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-1464")
	public void testQueryMetadataRetrievalWithFetching(SessionFactoryScope scope) {
		// HHH-1464 : there was a problem due to the fact they we polled
		// the shallow version of the query plan to get the metadata.
		scope.inSession(
				session -> {
					final Query query = session.createQuery( "from Animal a inner join fetch a.mother" );
					final SqmSelectStatement<?> sqmStatement =
							(SqmSelectStatement<?>) query.unwrap( SqmQuery.class ).getSqmStatement();
					assertThat( sqmStatement.getQuerySpec().getSelectClause().getSelections().size() ).isEqualTo( 1 );
					final SqmSelection<?> selection = sqmStatement.getQuerySpec().getSelectClause().getSelections()
							.get( 0 );
					final SqmExpressible<?> selectionType = selection.getSelectableNode().getNodeType();
					assertThat( selectionType ).isInstanceOf( EntityDomainType.class );
					assertThat( selectionType.getExpressibleJavaType().getJavaTypeClass() ).isEqualTo( Animal.class );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-429")
	@SuppressWarnings({"unchecked"})
	public void testSuperclassPropertyReferenceAfterCollectionIndexedAccess(SessionFactoryScope scope) {
		// note: simply performing syntax checking in the db
		scope.inTransaction(
				session -> {
					Mammal tiger = new Mammal();
					tiger.setDescription( "Tiger" );
					session.persist( tiger );
					Mammal mother = new Mammal();
					mother.setDescription( "Tiger's mother" );
					mother.setBodyWeight( 4.0f );
					mother.addOffspring( tiger );
					session.persist( mother );
					Zoo zoo = new Zoo();
					zoo.setName( "Austin Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.getMammals().put( "tiger", tiger );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery(
							"from Zoo zoo where zoo.mammals['tiger'].mother.bodyWeight > 3.0f" ).list();
					assertThat( results.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testJoinFetchCollectionOfValues(SessionFactoryScope scope) {
		// note: simply performing syntax checking in the db
		scope.inTransaction(
				session ->
						session.createQuery( "select h from Human as h join fetch h.nickNames" ).list()
		);
	}

	@Test
	public void testIntegerLiterals(SessionFactoryScope scope) {
		// note: simply performing syntax checking in the db
		scope.inTransaction(
				session -> {
					session.createQuery( "from Foo where long = 1" ).list();
					session.createQuery( "from Foo where long = " + Integer.MIN_VALUE ).list();
					session.createQuery( "from Foo where long = " + Integer.MAX_VALUE ).list();
					session.createQuery( "from Foo where long = 1L" ).list();
					session.createQuery( "from Foo where long = " + (Long.MIN_VALUE + 1) + "L" ).list();
					session.createQuery( "from Foo where long = " + Long.MAX_VALUE + "L" ).list();
					session.createQuery( "from Foo where integer = " + (Long.MIN_VALUE + 1) ).list();
				}
		);
	}

	@Test
	public void testDecimalLiterals(SessionFactoryScope scope) {
		// note: simply performing syntax checking in the db
		scope.inTransaction(
				session -> {
					session.createQuery( "from Animal where bodyWeight > 100.0e-10" ).list();
					session.createQuery( "from Animal where bodyWeight > 100.0E-10" ).list();
					session.createQuery( "from Animal where bodyWeight > 100.001f" ).list();
					session.createQuery( "from Animal where bodyWeight > 100.001F" ).list();
					session.createQuery( "from Animal where bodyWeight > 100.001d" ).list();
					session.createQuery( "from Animal where bodyWeight > 100.001D" ).list();
					session.createQuery( "from Animal where bodyWeight > .001f" ).list();
					session.createQuery( "from Animal where bodyWeight > 100e-10" ).list();
					session.createQuery( "from Animal where bodyWeight > .01E-10" ).list();
					session.createQuery( "from Animal where bodyWeight > 1e-38" ).list();
				}
		);
	}

	@Test
	public void testNakedPropertyRef(SessionFactoryScope scope) {
		// note: simply performing syntax and column/table resolution checking in the db
		scope.inTransaction(
				session -> {
					session.createQuery( "from Animal where bodyWeight = bodyWeight" ).list();
					session.createQuery( "select bodyWeight from Animal" ).list();
					session.createQuery( "select max(bodyWeight) from Animal" ).list();
				}
		);
	}

	@Test
	public void testNakedComponentPropertyRef(SessionFactoryScope scope) {
		// note: simply performing syntax and column/table resolution checking in the db
		scope.inTransaction(
				session -> {
					session.createQuery( "from Human where name.first = 'Gavin'" ).list();
					session.createQuery( "select name from Human" ).list();
					session.createQuery( "select upper(h.name.first) from Human as h" ).list();
					session.createQuery( "select upper(name.first) from Human" ).list();
				}
		);
	}

	@Test
	public void testNakedImplicitJoins(SessionFactoryScope scope) {
		// note: simply performing syntax and column/table resolution checking in the db
		scope.inTransaction(
				session ->
						session.createQuery( "from Animal where mother.father.id = 1" ).list()
		);
	}

	@Test
	public void testNakedEntityAssociationReference(SessionFactoryScope scope) {
		// note: simply performing syntax and column/table resolution checking in the db
		scope.inTransaction(
				session -> {
					if ( session.getDialect() instanceof HANADialect ) {
						session.createQuery( "from Animal where mother is null" ).list();
					}
					else {
						session.createQuery( "from Animal where mother = :mother" ).setParameter( "mother", null )
								.list();
					}
				}
		);
	}

	@Test
	public void testNakedMapIndex(SessionFactoryScope scope) {
		// note: simply performing syntax and column/table resolution checking in the db
		scope.inTransaction(
				session ->
						session.createQuery( "from Zoo where mammals['dog'].description like '%black%'" ).list()

		);
	}

	@Test
	public void testInvalidFetchSemantics(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "select mother from Human a left join fetch a.mother mother" )
											.list();
									fail( "invalid fetch semantic allowed!" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException e) {
								}
							}
					);

					scope.inTransaction(
							s,
							session -> {
								try {
									s.createQuery( "select mother from Human a left join fetch a.mother mother" )
											.list();
									fail( "invalid fetch semantic allowed!" );
								}
								catch (IllegalArgumentException e) {
									assertTyping( QueryException.class, e.getCause() );
								}
								catch (QueryException e) {
								}
							}
					);
				}
		);
	}

	@Test
	public void testArithmetic(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "Melbourne Zoo" );
					session.persist( zoo );
					session.createQuery( "select 2*2*2*2*(2*2) from Zoo" ).uniqueResult();
					session.createQuery( "select 2 / (1+1) from Zoo" ).uniqueResult();
					int result0 = session.createQuery( "select 2 - (1+1) from Zoo", Integer.class ).uniqueResult();
					int result1 = session.createQuery( "select 2 - 1 + 1 from Zoo", Integer.class ).uniqueResult();
					int result2 = session.createQuery( "select 2 * (1-1) from Zoo", Integer.class ).uniqueResult();
					int result3 = session.createQuery( "select 4 / (2 * 2) from Zoo", Integer.class ).uniqueResult();
					int result4 = session.createQuery( "select 4 / 2 * 2 from Zoo", Integer.class ).uniqueResult();
					int result5 = session.createQuery( "select 2 * (2/2) from Zoo", Integer.class ).uniqueResult();
					int result6 = session.createQuery( "select 2 * (2/2+1) from Zoo", Integer.class ).uniqueResult();
					assertThat( result0 ).isEqualTo( 0 );
					assertThat( result1 ).isEqualTo( 2 );
					assertThat( result2 ).isEqualTo( 0 );
					assertThat( result3 ).isEqualTo( 1 );
					assertThat( result4 ).isEqualTo( 4 );
					assertThat( result5 ).isEqualTo( 2 );
					assertThat( result6 ).isEqualTo( 4 );
				}
		);
	}

	@Test
	public void testNestedCollectionFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"from Animal a left join fetch a.offspring o left join fetch o.offspring where a.mother.id = 1 order by a.description" )
							.list();
					session.createQuery(
									"from Zoo z left join fetch z.animals a left join fetch a.offspring where z.name ='MZ' order by a.description" )
							.list();
					session.createQuery(
									"from Human h left join fetch h.pets a left join fetch a.offspring where h.name.first ='Gavin' order by a.description" )
							.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	@SuppressWarnings({"unchecked"})
	public void testSelectClauseSubselect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "Melbourne Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.setAnimals( new HashMap() );
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					plat.setZoo( zoo );
					plat.setSerialNumber( "plat123" );
					zoo.getMammals().put( "Platypus", plat );
					zoo.getAnimals().put( "plat123", plat );
					session.persist( plat );
					session.persist( zoo );

					session.createQuery( "select (select max(z.id) from a.zoo z) from Animal a" ).list();
					session.createQuery( "select (select max(z.id) from a.zoo z where z.name=:name) from Animal a" )
							.setParameter( "name", "Melbourne Zoo" ).list();

					session.remove( plat );
					session.remove( zoo );
				}
		);
	}

	@Test
	public void testInitProxy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					session.persist( plat );
					session.flush();
					session.clear();
					plat = session.getReference( Mammal.class, plat.getId() );
					assertThat( Hibernate.isInitialized( plat ) ).isFalse();
					Object plat2 = session.createQuery( "from Animal a" ).uniqueResult();
					assertThat( plat2 ).isSameAs( plat );
					assertThat( Hibernate.isInitialized( plat ) ).isTrue();
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testSelectClauseImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "The Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.setAnimals( new HashMap() );
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					plat.setZoo( zoo );
					plat.setSerialNumber( "plat123" );
					zoo.getMammals().put( "Platypus", plat );
					zoo.getAnimals().put( "plat123", plat );
					session.persist( plat );
					session.persist( zoo );
					session.flush();
					session.clear();

					Query q = session.createQuery( "select distinct a.zoo from Animal a where a.zoo is not null" );

					verifyAnimalZooSelection( q );

					zoo = (Zoo) q.list().get( 0 );
					assertThat( zoo.getMammals().size() ).isEqualTo( 1 );
					assertThat( zoo.getAnimals().size() ).isEqualTo( 1 );
				}
		);
	}

	private static void verifyAnimalZooSelection(Query q) {
		final SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) q.unwrap( SqmQuery.class ).getSqmStatement();
		final SqmSelection<?> sqmSelection = sqmStatement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( sqmSelection.getSelectableNode() ).isInstanceOf( SqmPath.class );
		final SqmPath<?> selectedPath = (SqmPath<?>) sqmSelection.getSelectableNode();
		assertThat( selectedPath.getReferencedPathSource() ).isInstanceOf( EntitySqmPathSource.class );
		final EntitySqmPathSource selectedAttr = (EntitySqmPathSource) selectedPath.getReferencedPathSource();
		assertThat( selectedAttr.getPathName() ).isEqualTo( "zoo" );
		assertThat( selectedAttr.getPathType() ).isInstanceOf( EntityDomainType.class );
		final EntityDomainType<?> zooType = (EntityDomainType<?>) selectedAttr.getPathType();
		assertThat( zooType.getHibernateEntityName() ).isEqualTo( Zoo.class.getName() );
	}

	@Test
	@JiraKey(value = "HHH-9305")
	@SuppressWarnings({"unchecked"})
	public void testSelectClauseImplicitJoinOrderByJoinedProperty(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "The Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.setAnimals( new HashMap() );
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					plat.setZoo( zoo );
					plat.setSerialNumber( "plat123" );
					zoo.getMammals().put( "Platypus", plat );
					zoo.getAnimals().put( "plat123", plat );
					Zoo otherZoo = new Zoo();
					otherZoo.setName( "The Other Zoo" );
					otherZoo.setMammals( new HashMap() );
					otherZoo.setAnimals( new HashMap() );
					Mammal zebra = new Mammal();
					zebra.setBodyWeight( 110f );
					zebra.setDescription( "Zebra" );
					zebra.setZoo( otherZoo );
					zebra.setSerialNumber( "zebra123" );
					otherZoo.getMammals().put( "Zebra", zebra );
					otherZoo.getAnimals().put( "zebra123", zebra );
					Mammal elephant = new Mammal();
					elephant.setBodyWeight( 550f );
					elephant.setDescription( "Elephant" );
					elephant.setZoo( otherZoo );
					elephant.setSerialNumber( "elephant123" );
					otherZoo.getMammals().put( "Elephant", elephant );
					otherZoo.getAnimals().put( "elephant123", elephant );
					session.persist( plat );
					session.persist( zoo );
					session.persist( zebra );
					session.persist( elephant );
					session.persist( otherZoo );
					session.flush();
					session.clear();

					Query q = session.createQuery(
							"select a.zoo from Animal a where a.zoo is not null order by a.zoo.name" );

					verifyAnimalZooSelection( q );

					List<Zoo> zoos = (List<Zoo>) q.list();
					assertThat( zoos.size() ).isEqualTo( 2 );
					assertThat( zoos.get( 0 ).getName() ).isEqualTo( otherZoo.getName() );
					assertThat( zoos.get( 0 ).getMammals().size() ).isEqualTo( 2 );
					assertThat( zoos.get( 0 ).getAnimals().size() ).isEqualTo( 2 );
					assertThat( zoos.get( 1 ).getName() ).isEqualTo( zoo.getName() );
					assertThat( zoos.get( 1 ).getMammals().size() ).isEqualTo( 1 );
					assertThat( zoos.get( 1 ).getAnimals().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testSelectClauseDistinctImplicitJoinOrderByJoinedProperty(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "The Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.setAnimals( new HashMap() );
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					plat.setZoo( zoo );
					plat.setSerialNumber( "plat123" );
					zoo.getMammals().put( "Platypus", plat );
					zoo.getAnimals().put( "plat123", plat );
					Zoo otherZoo = new Zoo();
					otherZoo.setName( "The Other Zoo" );
					otherZoo.setMammals( new HashMap() );
					otherZoo.setAnimals( new HashMap() );
					Mammal zebra = new Mammal();
					zebra.setBodyWeight( 110f );
					zebra.setDescription( "Zebra" );
					zebra.setZoo( otherZoo );
					zebra.setSerialNumber( "zebra123" );
					otherZoo.getMammals().put( "Zebra", zebra );
					otherZoo.getAnimals().put( "zebra123", zebra );
					Mammal elephant = new Mammal();
					elephant.setBodyWeight( 550f );
					elephant.setDescription( "Elephant" );
					elephant.setZoo( otherZoo );
					elephant.setSerialNumber( "elephant123" );
					otherZoo.getMammals().put( "Elephant", elephant );
					otherZoo.getAnimals().put( "elephant123", elephant );
					session.persist( plat );
					session.persist( zoo );
					session.persist( zebra );
					session.persist( elephant );
					session.persist( otherZoo );
					session.flush();
					session.clear();

					Query q = session.createQuery(
							"select distinct a.zoo from Animal a where a.zoo is not null order by a.zoo.name" );

					verifyAnimalZooSelection( q );

					List<Zoo> zoos = (List<Zoo>) q.list();
					assertThat( zoos.size() ).isEqualTo( 2 );
					assertThat( zoos.get( 0 ).getName() ).isEqualTo( otherZoo.getName() );
					assertThat( zoos.get( 0 ).getMammals().size() ).isEqualTo( 2 );
					assertThat( zoos.get( 0 ).getAnimals().size() ).isEqualTo( 2 );
					assertThat( zoos.get( 1 ).getName() ).isEqualTo( zoo.getName() );
					assertThat( zoos.get( 1 ).getMammals().size() ).isEqualTo( 1 );
					assertThat( zoos.get( 1 ).getAnimals().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testSelectClauseImplicitJoinWithIterate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Zoo zoo = new Zoo();
					zoo.setName( "The Zoo" );
					zoo.setMammals( new HashMap() );
					zoo.setAnimals( new HashMap() );
					Mammal plat = new Mammal();
					plat.setBodyWeight( 11f );
					plat.setDescription( "Platypus" );
					plat.setZoo( zoo );
					plat.setSerialNumber( "plat123" );
					zoo.getMammals().put( "Platypus", plat );
					zoo.getAnimals().put( "plat123", plat );
					session.persist( plat );
					session.persist( zoo );
					session.flush();
					session.clear();

					Query q = session.createQuery( "select distinct a.zoo from Animal a where a.zoo is not null" );

					verifyAnimalZooSelection( q );

					zoo = (Zoo) q.list().iterator().next();
					assertThat( zoo.getMammals().size() ).isEqualTo( 1 );
					assertThat( zoo.getAnimals().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testComponentOrderBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human1 = genSimpleHuman( "John", "Jacob" );
					session.persist( human1 );
					Long id1 = human1.getId();
					Human human2 = genSimpleHuman( "Jingleheimer", "Schmidt" );
					session.persist( human2 );
					Long id2 = human2.getId();

					session.flush();

					// the component is defined with the firstName column first...
					List results = session.createQuery( "from Human as h order by h.name" ).list();
					assertThat( results.size() ).as( "Incorrect return count" ).isEqualTo( 2 );

					Human h1 = (Human) results.get( 0 );
					Human h2 = (Human) results.get( 1 );

					assertThat( h1.getId() ).as( "Incorrect ordering" ).isEqualTo( id2 );
					assertThat( h2.getId() ).as( "Incorrect ordering" ).isEqualTo( id1 );
				}
		);
	}

	@Test
	public void testOrderedWithCustomColumnReadAndWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntityWithAssociation first = new SimpleEntityWithAssociation();
					first.setNegatedNumber( 1 );
					session.persist( first );
					SimpleEntityWithAssociation second = new SimpleEntityWithAssociation();
					second.setNegatedNumber( 2 );
					session.persist( second );
					session.flush();

					// Check order via SQL. Numbers are negated in the DB, so second comes first.
					List listViaSql = session.createNativeQuery( "select ID from SIMPLE_1 order by negated_num" )
							.list();
					assertThat( listViaSql.size() ).isEqualTo( 2 );
					assertThat( ((Number) listViaSql.get( 0 )).longValue() ).isEqualTo( second.getId().longValue() );
					assertThat( ((Number) listViaSql.get( 1 )).longValue() ).isEqualTo( first.getId().longValue() );

					// Check order via HQL. Now first comes first b/c the read negates the DB negation.
					List listViaHql = session.createQuery( "from SimpleEntityWithAssociation order by negatedNumber" )
							.list();
					assertThat( listViaHql.size() ).isEqualTo( 2 );
					assertThat( ((SimpleEntityWithAssociation) listViaHql.get( 0 )).getId() )
							.isEqualTo( first.getId() );
					assertThat( ((SimpleEntityWithAssociation) listViaHql.get( 1 )).getId() )
							.isEqualTo( second.getId() );
				}
		);
	}

	@Test
	public void testHavingWithCustomColumnReadAndWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntityWithAssociation first = new SimpleEntityWithAssociation();
					first.setNegatedNumber( 5 );
					first.setName( "simple" );
					session.persist( first );
					SimpleEntityWithAssociation second = new SimpleEntityWithAssociation();
					second.setNegatedNumber( 10 );
					second.setName( "simple" );
					session.persist( second );
					SimpleEntityWithAssociation third = new SimpleEntityWithAssociation();
					third.setNegatedNumber( 20 );
					third.setName( "complex" );
					session.persist( third );
					session.flush();

					// Check order via HQL. Now first comes first b/c the read negates the DB negation.
					Number r = (Number) session.createQuery(
							"select sum(negatedNumber) from SimpleEntityWithAssociation " +
							"group by name having sum(negatedNumber) < 20" ).uniqueResult();
					assertThat( r.intValue() ).isEqualTo( 15 );
				}
		);

	}

	@Test
	public void testLoadSnapshotWithCustomColumnReadAndWrite(SessionFactoryScope scope) {
		// Exercises entity snapshot load when select-before-update is true.
		Image image = new Image();
		scope.inTransaction(
				session -> {
					final double SIZE_IN_KB = 1536d;
					final double SIZE_IN_MB = SIZE_IN_KB / 1024d;
					image.setName( "picture.gif" );
					image.setSizeKb( SIZE_IN_KB );
					session.persist( image );
					session.flush();

					// Value returned by Oracle is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double sizeViaSql = ((Number) session.createNativeQuery( "select size_mb from image" )
							.uniqueResult()).doubleValue();
					assertThat( sizeViaSql ).isEqualTo( SIZE_IN_MB, Offset.offset( 0.01d ) );
				}
		);

		scope.inTransaction(
				session -> {
					final double NEW_SIZE_IN_KB = 2048d;
					final double NEW_SIZE_IN_MB = NEW_SIZE_IN_KB / 1024d;
					image.setSizeKb( NEW_SIZE_IN_KB );
					session.merge( image );
					session.flush();

					Double sizeViaSql = ((Number) session.createNativeQuery( "select size_mb from image" )
							.uniqueResult()).doubleValue();
					assertThat( sizeViaSql ).isEqualTo( NEW_SIZE_IN_MB, Offset.offset( 0.01d ) );
				}
		);
	}

	private Human genSimpleHuman(String fName, String lName) {
		Human h = new Human();
		h.setName( new Name( fName, 'X', lName ) );
		return h;
	}

	@Test
	public void testCastInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal a = new Animal();
					a.setBodyWeight( 12.4f );
					a.setDescription( "an animal" );
					session.persist( a );
					Object bodyWeight = session.createQuery( "select cast(bodyWeight as integer) from Animal" )
							.uniqueResult();
					assertThat( bodyWeight ).isInstanceOf( Integer.class );
					assertThat( bodyWeight ).isEqualTo( 12 );

					bodyWeight = session.createQuery( "select cast(bodyWeight as big_decimal) from Animal" )
							.uniqueResult();
					assertThat( bodyWeight ).isInstanceOf( BigDecimal.class );
					assertThat( ((BigDecimal) bodyWeight).floatValue() )
							.isEqualTo( a.getBodyWeight(), Offset.offset( .01F ) );

					Object literal = session.createQuery( "select cast(10000000 as big_integer) from Animal" )
							.uniqueResult();
					assertThat( literal ).isInstanceOf( BigInteger.class );
					assertThat( literal ).isEqualTo( BigInteger.valueOf( 10000000 ) );
				}
		);
	}

	@Test
	public void testNumericExpressionReturnTypes(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal a = new Animal();
					a.setBodyWeight( 12.4f );
					a.setDescription( "an animal" );
					session.persist( a );

					Object result;

					// addition ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					result = session.createQuery( "select 1 + 1 from Animal as a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Integer.class );
					assertThat( result ).isEqualTo( 2 );

					result = session.createQuery( "select 1 + 1L from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Long.class );
					assertThat( result ).isEqualTo( 2L );

					result = session.createQuery( "select 1 + 1BI from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigInteger.class );
					assertThat( result ).isEqualTo( BigInteger.valueOf( 2 ) );

					result = session.createQuery( "select 1 + 1F from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Float.class );
					assertThat( result ).isEqualTo( 2F );

					result = session.createQuery( "select 1 + 1D from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Double.class );
					assertThat( result ).isEqualTo( 2D );

					result = session.createQuery( "select 1 + 1BD from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigDecimal.class );
					assertThat( result ).isEqualTo( BigDecimal.valueOf( 2 ) );

					result = session.createQuery( "select 1F + 1D from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Double.class );
					assertThat( result ).isEqualTo( 2D );

					result = session.createQuery( "select 1F + 1BD from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Float.class );
					assertThat( result ).isEqualTo( 2F );

					// subtraction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					result = session.createQuery( "select 1 - 1 from Animal as a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Integer.class );
					assertThat( result ).isEqualTo( 0 );

					result = session.createQuery( "select 1 - 1L from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Long.class );
					assertThat( result ).isEqualTo( 0L );

					result = session.createQuery( "select 1 - 1BI from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigInteger.class );
					assertThat( result ).isEqualTo( BigInteger.valueOf( 0 ) );

					result = session.createQuery( "select 1 - 1F from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Float.class );
					assertThat( result ).isEqualTo( 0F );

					result = session.createQuery( "select 1 - 1D from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Double.class );
					assertThat( result ).isEqualTo( 0D );

					result = session.createQuery( "select 1 - 1BD from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigDecimal.class );
					assertThat( result ).isEqualTo( BigDecimal.valueOf( 0 ) );

					result = session.createQuery( "select 1F - 1D from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Double.class );
					assertThat( result ).isEqualTo( 0D );

					result = session.createQuery( "select 1F - 1BD from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Float.class );
					assertThat( result ).isEqualTo( 0F );

					// multiplication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
					result = session.createQuery( "select 1 * 1 from Animal as a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Integer.class );
					assertThat( result ).isEqualTo( 1 );

					result = session.createQuery( "select 1 * 1L from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Long.class );
					assertThat( result ).isEqualTo( 1L );

					result = session.createQuery( "select 1 * 1BI from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigInteger.class );
					;
					assertThat( result ).isEqualTo( BigInteger.valueOf( 1 ) );

					result = session.createQuery( "select 1 * 1F from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Float.class );
					assertThat( result ).isEqualTo( 1F );

					result = session.createQuery( "select 1 * 1D from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( Double.class );
					assertThat( result ).isEqualTo( 1D );

					result = session.createQuery( "select 1 * 1BD from Animal a" ).uniqueResult();
					assertThat( result ).isInstanceOf( BigDecimal.class );
					assertThat( result ).isEqualTo( BigDecimal.valueOf( 1 ) );
				}
		);
	}

	@Test
	public void testAliases(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal a = new Animal();
					a.setBodyWeight( 12.4f );
					a.setDescription( "an animal" );
					session.persist( a );

					Query<?> q = session.createQuery( "select a.bodyWeight as abw, a.description from Animal a" );
					SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) q.unwrap( SqmQuery.class )
							.getSqmStatement();
					List<SqmSelection<?>> selections = sqmStatement.getQuerySpec().getSelectClause().getSelections();
					assertThat( selections.size() ).isEqualTo( 2 );
					assertThat( selections.get( 0 ).getAlias() ).isEqualTo( "abw" );
					assertThat( selections.get( 1 ).getAlias() ).isNull();

					q = session.createQuery( "select count(*), avg(a.bodyWeight) as avg from Animal a" );
					sqmStatement = (SqmSelectStatement<?>) q.unwrap( SqmQuery.class ).getSqmStatement();
					selections = sqmStatement.getQuerySpec().getSelectClause().getSelections();
					assertThat( selections.size() ).isEqualTo( 2 );

					assertThat( selections.get( 0 ) ).isNotNull();
					assertThat( selections.get( 0 ).getAlias() ).isNull();
					assertThat( selections.get( 0 ).getSelectableNode() ).isInstanceOf( SqmFunction.class );
					assertThat( ((SqmFunction) selections.get( 0 ).getSelectableNode()).getFunctionName() ).isEqualTo(
							"count" );

					assertThat( selections.get( 1 ) ).isNotNull();
					assertThat( selections.get( 1 ).getAlias() ).isNotNull();
					assertThat( selections.get( 1 ).getAlias() ).isEqualTo( "avg" );
					assertThat( selections.get( 1 ).getSelectableNode() ).isInstanceOf( SqmFunction.class );
					assertThat( ((SqmFunction) selections.get( 1 ).getSelectableNode()).getFunctionName() ).isEqualTo(
							"avg" );

				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	public void testParameterMixing(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery(
										"from Animal a where a.description = ?1 and a.bodyWeight = ?2 or a.bodyWeight = :bw" )
								.setParameter( 1, "something" )
								.setParameter( 2, 12345f )
								.setParameter( "bw", 123f )
								.list()

		);
	}

	@Test
	public void testOrdinalParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Animal a where a.description = ?1 and a.bodyWeight = ?2" )
							.setParameter( 1, "something" )
							.setParameter( 2, 123f )
							.list();
					session.createQuery( "from Animal a where a.bodyWeight in (?1, ?2)" )
							.setParameter( 1, 999f )
							.setParameter( 2, 123f )
							.list();
				}
		);
	}

	@Test
	public void testIndexParams(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Zoo zoo where zoo.mammals[:name].id = :id" )
							.setParameter( "name", "Walrus" )
							.setParameter( "id", Long.valueOf( 123 ) )
							.list();
					session.createQuery( "from Zoo zoo where zoo.mammals[:name].bodyWeight > :w" )
							.setParameter( "name", "Walrus" )
							.setParameter( "w", new Float( 123.32 ) )
							.list();
					session.createQuery( "from Zoo zoo where zoo.animals[:sn].mother.bodyWeight < :mw" )
							.setParameter( "sn", "ant-123" )
							.setParameter( "mw", new Float( 23.32 ) )
							.list();
		/*s.createQuery("from Zoo zoo where zoo.animals[:sn].description like :desc and zoo.animals[:sn].bodyWeight > :wmin and zoo.animals[:sn].bodyWeight < :wmax")
			.setParameter("sn", "ant-123")
			.setParameter("desc", "%big%")
			.setParameter("wmin", new Float(123.32))
			.setParameter("wmax", new Float(167.89))
			.list();*/
		/*s.createQuery("from Human where addresses[:type].city = :city and addresses[:type].country = :country")
			.setParameter("type", "home")
			.setParameter("city", "Melbourne")
			.setParameter("country", "Australia")
			.list();*/
				}
		);
	}

	@Test
	public void testAggregation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setBodyWeight( (float) 74.0 );
					h.setHeightInches( 120.5 );
					h.setDescription( "Me" );
					h.setName( new Name( "Gavin", 'A', "King" ) );
					h.setNickName( "Oney" );
					session.persist( h );
					Double sum = (Double) session.createQuery( "select sum(h.bodyWeight) from Human h" ).uniqueResult();
					Double avg = (Double) session.createQuery( "select avg(h.heightInches) from Human h" )
							.uniqueResult();    // uses custom read and write for column
					assertThat( sum.floatValue() ).isEqualTo( 74.0F, Offset.offset( 0.01F ) );
					assertThat( avg.doubleValue() ).isEqualTo( 120.5D, Offset.offset( 0.01D ) );
					Long id = (Long) session.createQuery( "select max(a.id) from Animal a" ).uniqueResult();
					assertThat( id ).isNotNull();
					session.remove( h );
				}
		);


		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setFloatValue( 2.5F );
					h.setIntValue( 1 );
					session.persist( h );
					Human h2 = new Human();
					h2.setFloatValue( 2.5F );
					h2.setIntValue( 2 );
					session.persist( h2 );
					Object[] results = (Object[]) session.createQuery(
									"select sum(h.floatValue), avg(h.floatValue), sum(h.intValue), avg(h.intValue) from Human h" )
							.uniqueResult();
					// spec says sum() on a float or double value should result in double
					assertThat( results[0] ).isInstanceOf( Double.class );
					assertThat( results[0] ).isEqualTo( 5D );
					// avg() should return a double
					assertThat( results[1] ).isInstanceOf( Double.class );
					assertThat( results[1] ).isEqualTo( 2.5D );
					// spec says sum() on short, int or long should result in long
					assertThat( results[2] ).isInstanceOf( Long.class );
					assertThat( results[2] ).isEqualTo( 3L );
					// avg() should return a double
					assertThat( results[3] ).isInstanceOf( Double.class );
					assertThat( results[3] ).isEqualTo( 1.5D );
				}
		);
	}

	@Test
	public void testSelectClauseCase(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setBodyWeight( (float) 74.0 );
					h.setHeightInches( 120.5 );
					h.setDescription( "Me" );
					h.setName( new Name( "Gavin", 'A', "King" ) );
					h.setNickName( "Oney" );
					session.persist( h );
					String name = (String) session.createQuery(
									"select case nickName when 'Oney' then 'gavin' when 'Turin' then 'christian' else nickName end from Human" )
							.uniqueResult();
					assertThat( name ).isEqualTo( "gavin" );
					String result = (String) session.createQuery(
									"select case when bodyWeight > 100 then 'fat' else 'skinny' end from Human" )
							.uniqueResult();
					assertThat( result ).isEqualTo( "skinny" );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void testImplicitPolymorphism(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product = new Product();
					product.setDescription( "My Product" );
					product.setNumberAvailable( 10 );
					product.setPrice( new BigDecimal( 123 ) );
					product.setProductId( "4321" );
					session.persist( product );

					List list = session.createQuery( "from java.lang.Object" ).list();
					assertThat( list.size() ).isEqualTo( 1 );

					session.remove( product );

					list = session.createQuery( "from java.lang.Object" ).list();
					assertThat( list.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCoalesce(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Human h where coalesce(h.nickName, h.name.first, h.name.last) = 'max'" )
							.list();
					session.createQuery( "select nullif(nickName, '1e1') from Human" ).list();
				}
		);
	}

	@Test
	public void testStr(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal an = new Animal();
					an.setBodyWeight( 123.45f );
					session.persist( an );
					String str = (String) session.createQuery(
									"select str(an.bodyWeight) from Animal an where str(an.bodyWeight) like '%1%'" )
							.uniqueResult();
					BigDecimal value = new BigDecimal( str, new MathContext( 4, RoundingMode.DOWN ) );
					assertEquals( new BigDecimal( "123.4" ), value );

					String dateStr1 = (String) session.createQuery( "select str(current_date) from Animal" )
							.uniqueResult();
					String dateStr2 = (String) session.createQuery(
									"select str(year(current_date))||'-'||str(month(current_date))||'-'||str(day(current_date)) from Animal" )
							.uniqueResult();
					String[] dp1 = StringHelper.split( "-", dateStr1 );
					String[] dp2 = StringHelper.split( "-", dateStr2 );
					for ( int i = 0; i < 3; i++ ) {
						if ( dp1[i].startsWith( "0" ) ) {
							dp1[i] = dp1[i].substring( 1 );
						}
						assertThat( dp2[i] ).isEqualTo( dp1[i] );
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = MySQLDialect.class)
	@SkipForDialect(dialectClass = DB2Dialect.class)
	public void testCast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Human h where h.nickName like 'G%'" ).list();
					session.createQuery( "from Animal a where cast(a.bodyWeight as string) like '1.%'" ).list();
					session.createQuery( "from Animal a where cast(a.bodyWeight as integer) = 1" ).list();
				}
		);
	}

	@Test
	public void testExtract(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
									"select second(current_timestamp()), minute(current_timestamp()), hour(current_timestamp()) from Mammal m" )
							.list();
					session.createQuery(
							"select day(m.birthdate), month(m.birthdate), year(m.birthdate) from Mammal m" ).list();
					if ( !(session.getDialect() instanceof DB2Dialect) ) { //no ANSI extract
						session.createQuery(
										"select extract(second from current_timestamp()), extract(minute from current_timestamp()), extract(hour from current_timestamp()) from Mammal m" )
								.list();
						session.createQuery(
										"select extract(day from m.birthdate), extract(month from m.birthdate), extract(year from m.birthdate) from Mammal m" )
								.list();
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testSelectExpressions(SessionFactoryScope scope) {
		createTestBaseData( scope );
		scope.inTransaction(
				session -> {
					Human h = new Human();
					h.setName( new Name( "Gavin", 'A', "King" ) );
					h.setNickName( "Oney" );
					h.setBodyWeight( 1.0f );
					session.persist( h );
					List results = session.createQuery(
									"select 'found', lower(h.name.first) from Human h where lower(h.name.first) = 'gavin'" )
							.list();
					results = session.createQuery(
									"select 'found', lower(h.name.first) from Human h where concat(h.name.first, ' ', h.name.initial, ' ', h.name.last) = 'Gavin A King'" )
							.list();
					results = session.createQuery(
									"select 'found', lower(h.name.first) from Human h where h.name.first||' '||h.name.initial||' '||h.name.last = 'Gavin A King'" )
							.list();
					results = session.createQuery( "select a.bodyWeight + m.bodyWeight from Animal a join a.mother m" )
							.list();
					results = session.createQuery(
							"select 2.0 * (a.bodyWeight + m.bodyWeight) from Animal a join a.mother m" ).list();
					results = session.createQuery(
							"select sum(a.bodyWeight + m.bodyWeight) from Animal a join a.mother m" ).list();
					results = session.createQuery( "select sum(a.mother.bodyWeight * 2.0) from Animal a" ).list();
					results = session.createQuery(
							"select concat(h.name.first, ' ', h.name.initial, ' ', h.name.last) from Human h" ).list();
					results = session.createQuery(
							"select h.name.first||' '||h.name.initial||' '||h.name.last from Human h" ).list();
					results = session.createQuery( "select nickName from Human" ).list();
					results = session.createQuery( "select lower(nickName) from Human" ).list();
					results = session.createQuery( "select abs(bodyWeight*-1) from Human" ).list();
					results = session.createQuery( "select upper(h.name.first||' ('||h.nickName||')') from Human h" )
							.list();
					results = session.createQuery( "select abs(a.bodyWeight-:param) from Animal a" )
							.setParameter( "param", new Float( 2.0 ) ).list();
					results = session.createQuery( "select abs(:param - a.bodyWeight) from Animal a" )
							.setParameter( "param", new Float( 2.0 ) ).list();
					results = session.createQuery( "select lower(upper('foo')) from Animal" ).list();
					results = session.createQuery( "select lower(upper('foo') || upper('bar')) from Animal" ).list();
					results = session.createQuery(
							"select sum(abs(bodyWeight - 1.0) * abs(length('ffobar')-3)) from Animal" ).list();
					session.remove( h );
				}
		);
		destroyTestBaseData( scope );
	}

	private void createTestBaseData(SessionFactoryScope scope) {
		Mammal m1 = new Mammal();
		Mammal m2 = new Mammal();
		scope.inTransaction(
				session -> {
					m1.setBodyWeight( 11f );
					m1.setDescription( "Mammal #1" );

					session.persist( m1 );

					m2.setBodyWeight( 9f );
					m2.setDescription( "Mammal #2" );
					m2.setMother( m1 );

					session.persist( m2 );
				}
		);
		createdAnimalIds.add( m1.getId() );
		createdAnimalIds.add( m2.getId() );
	}

	private void destroyTestBaseData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( Long createdAnimalId : createdAnimalIds ) {
						Animal animal = session.getReference( Animal.class, createdAnimalId );
						session.remove( animal );
					}
				}
		);
		createdAnimalIds.clear();
	}

	@Test
	public void testImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal a = new Animal();
					a.setBodyWeight( 0.5f );
					a.setBodyWeight( 1.5f );
					Animal b = new Animal();
					Animal mother = new Animal();
					mother.setBodyWeight( 10.0f );
					mother.addOffspring( a );
					mother.addOffspring( b );
					session.persist( a );
					session.persist( b );
					session.persist( mother );
					List list = session.createQuery(
							"from Animal a where a.mother.bodyWeight < 2.0 or a.mother.bodyWeight > 9.0" ).list();
					assertThat( list.size() ).isEqualTo( 2 );
					list = session.createQuery(
							"from Animal a where a.mother.bodyWeight > 2.0 and a.mother.bodyWeight > 9.0" ).list();
					assertThat( list.size() ).isEqualTo( 2 );
					session.remove( b );
					session.remove( a );
					session.remove( mother );
				}
		);
	}

	@Test
	public void testFromOnly(SessionFactoryScope scope) {
		createTestBaseData( scope );
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Animal" ).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isInstanceOf( Animal.class );

				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	public void testSimpleSelect(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select a from Animal as a" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect result type" ).isInstanceOf( Animal.class );
				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	public void testEntityPropertySelect(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );
		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select a.mother from Animal as a" ).list();
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Animal.class );
				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	public void testWhere(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Animal an where an.bodyWeight > 10" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );

					results = session.createQuery( "from Animal an where not an.bodyWeight > 10" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );

					results = session.createQuery( "from Animal an where an.bodyWeight between 0 and 10" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );

					results = session.createQuery( "from Animal an where an.bodyWeight not between 0 and 10" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );

					results = session.createQuery( "from Animal an where sqrt(an.bodyWeight)/2 > 10" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 0 );

					results = session.createQuery(
									"from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	public void testEntityFetching(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Animal an join fetch an.mother" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Animal.class );
					Animal mother = ((Animal) results.get( 0 )).getMother();
					assertThat( mother != null && Hibernate.isInitialized( mother ) ).as( "fetch uninitialized" )
							.isTrue();

					results = session.createQuery( "select an from Animal an join fetch an.mother" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Animal.class );
					mother = ((Animal) results.get( 0 )).getMother();
					assertThat( mother != null && Hibernate.isInitialized( mother ) ).as( "fetch uninitialized" )
							.isTrue();
				}
		);

		destroyTestBaseData( scope );
	}

	@Test
	public void testCollectionFetching(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Animal an join fetch an.offspring" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Animal.class );
					Collection os = ((Animal) results.get( 0 )).getOffspring();
					assertThat( os != null && Hibernate.isInitialized( os ) && os.size() == 1 )
							.as( "fetch uninitialized" ).isTrue();

					results = session.createQuery( "select an from Animal an join fetch an.offspring" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Animal.class );
					os = ((Animal) results.get( 0 )).getOffspring();
					assertThat( os != null && Hibernate.isInitialized( os ) && os.size() == 1 )
							.as( "fetch uninitialized" ).isTrue();
				}
		);

		destroyTestBaseData( scope );
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJoinFetchedCollectionOfJoinedSubclass(SessionFactoryScope scope) throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		scope.inTransaction(
				session -> {
					session.persist( mammal );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Zoo z join fetch z.mammals" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Zoo.class );
					Zoo zooRead = (Zoo) results.get( 0 );
					assertThat( zooRead ).isEqualTo( zoo );
					assertThat( Hibernate.isInitialized( zooRead.getMammals() ) ).isTrue();
					Mammal mammalRead = (Mammal) zooRead.getMammals().get( "zebra" );
					assertThat( mammalRead ).isEqualTo( mammal );
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJoinedCollectionOfJoinedSubclass(SessionFactoryScope scope) throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		scope.inTransaction(
				session -> {
					session.persist( mammal );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select z, m from Zoo z join z.mammals m" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Object[].class );

					Object[] resultObjects = (Object[]) results.get( 0 );
					Zoo zooRead = (Zoo) resultObjects[0];
					Mammal mammalRead = (Mammal) resultObjects[1];
					assertThat( zooRead ).isEqualTo( zoo );
					assertThat( mammalRead ).isEqualTo( mammal );
				}
		);
	}

	@Test
	@SuppressWarnings({"unchecked"})
	public void testJoinedCollectionOfJoinedSubclassProjection(SessionFactoryScope scope) throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		scope.inTransaction(
				session -> {
					session.persist( mammal );
					session.persist( zoo );
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select z, m from Zoo z join z.mammals m" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Object[].class );
					Object[] resultObjects = (Object[]) results.get( 0 );
					Zoo zooRead = (Zoo) resultObjects[0];
					Mammal mammalRead = (Mammal) resultObjects[1];
					assertThat( zooRead ).isEqualTo( zoo );
					assertThat( mammalRead ).isEqualTo( mammal );
				}
		);
	}

	@Test
	public void testProjectionQueries(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		scope.inTransaction(
				session -> {
					List results = session.createQuery(
							"select an.mother.id, max(an.bodyWeight) from Animal an group by an.mother.id" ).list();
					// mysql returns nulls in this group by
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect result return type" ).isInstanceOf( Object[].class );
					assertThat( ((Object[]) results.get( 0 )).length ).as( "Incorrect return dimensions" )
							.isEqualTo( 2 );

				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class)
	public void testStandardFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product p = new Product();
					p.setDescription( "a product" );
					p.setPrice( new BigDecimal( 1.0 ) );
					p.setProductId( "abc123" );
					session.persist( p );
					Object[] result = (Object[]) session
							.createQuery( "select current_time(), current_date(), current_timestamp() from Product" )
							.uniqueResult();
					assertThat( result[0] ).isInstanceOf( Time.class );
					assertThat( result[1] ).isInstanceOf( Date.class );
					assertThat( result[2] ).isInstanceOf( Timestamp.class );
					assertThat( result[0] ).isNotNull();
					assertThat( result[1] ).isNotNull();
					assertThat( result[2] ).isNotNull();
				}
		);
	}

	@Test
	public void testDynamicInstantiationQueries(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		scope.inTransaction(
				session -> {
					List results = session.createQuery(
							"select new Animal(an.description, an.bodyWeight) from Animal an" ).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Animal.class );

					results = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( List.class );
					assertThat( ((List) results.get( 0 )).size() ).as( "Incorrect result size" ).isEqualTo( 2 );

					results = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( List.class );
					assertThat( ((List) results.get( 0 )).size() ).as( "Incorrect result size" ).isEqualTo( 2 );

					Object obj;

					results = session.createQuery( "select new map(an.description, an.bodyWeight) from Animal an" )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( Map.class );
					assertThat( ((Map) results.get( 0 )).size() ).as( "Incorrect result size" ).isEqualTo( 2 );

					assertThat( ((Map) results.get( 0 )).containsKey( "0" ) ).isTrue();
					assertThat( ((Map) results.get( 0 )).containsKey( "1" ) ).isTrue();

					results = session.createQuery(
									"select new map(an.description as descr, an.bodyWeight as bw) from Animal an" )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( Map.class );
					assertThat( ((Map) results.get( 0 )).size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( ((Map) results.get( 0 )).containsKey( "descr" ) ).isTrue();
					assertThat( ((Map) results.get( 0 )).containsKey( "bw" ) ).isTrue();

					try (ScrollableResults sr = session.createQuery(
							"select new map(an.description, an.bodyWeight) from Animal an" ).scroll()) {
						assertThat( sr.next() ).as( "Incorrect result size" ).isTrue();
						obj = sr.get();
						assertThat( obj ).as( "Incorrect return type" ).isInstanceOf( Map.class );
						assertThat( ((Map) obj).size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					}

					try (ScrollableResults sr = session.createQuery(
							"select new Animal(an.description, an.bodyWeight) from Animal an" ).scroll()) {
						assertThat( sr.next() ).as( "Incorrect result size" ).isTrue();
						assertThat( sr.get() ).as( "Incorrect return type" ).isInstanceOf( Animal.class );
					}

					// caching...
					QueryStatistics stats = scope.getSessionFactory().getStatistics()
							.getQueryStatistics( "select new Animal(an.description, an.bodyWeight) from Animal an" );
					results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" )
							.setCacheable( true )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( Animal.class, results.get( 0 ).getClass() );
					long initCacheHits = stats.getCacheHitCount();
					results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" )
							.setCacheable( true )
							.list();
					assertThat( stats.getCacheHitCount() ).as( "dynamic intantiation query not served from cache" )
							.isEqualTo( initCacheHits + 1 );
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( Animal.class, results.get( 0 ).getClass() );
				}
		);

		destroyTestBaseData( scope );
	}

	@Test
	@JiraKey(value = "HHH-9305")
	public void testDynamicInstantiationWithToOneQueries(SessionFactoryScope scope) throws Exception {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		scope.inTransaction(
				session -> {
					session.persist( title1 );
					session.persist( dept1 );
					session.persist( employee1 );
					session.persist( title2 );
					session.persist( employee2 );
				}
		);

		// There are 2 to-one associations: Employee.title and Employee.department.
		// It appears that adding an explicit join for one of these to-one associations keeps ANSI joins
		// at the beginning of the FROM clause, avoiding failures on DBs that cannot handle cross joins
		// interleaved with ANSI joins (e.g., PostgreSql).

		scope.inTransaction(
				session -> {
					List results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e inner join e.title"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, t.id, t.description, e.department, e.firstName) from Employee e inner join e.title t"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e inner join e.department"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, d, e.firstName) from Employee e inner join e.department d"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 1 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, d, e.firstName) from Employee e left outer join e.department d"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department inner join e.title"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, t.id, t.description, d, e.firstName) from Employee e left outer join e.department d inner join e.title t"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department left outer join e.title"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, t.id, t.description, d, e.firstName) from Employee e left outer join e.department d left outer join e.title t"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department order by e.title.description"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
					results = session.createQuery(
							"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department d order by e.title.description"
					).list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
				}
		);
	}

	@Test
	@SuppressWarnings("unused")
	public void testCachedJoinedAndJoinFetchedManyToOne(SessionFactoryScope scope) throws Exception {
		Animal a = new Animal();
		a.setDescription( "an animal" );

		Animal mother = new Animal();
		mother.setDescription( "a mother" );
		mother.addOffspring( a );
		a.setMother( mother );

		Animal offspring1 = new Animal();
		offspring1.setDescription( "offspring1" );
		a.addOffspring( offspring1 );
		offspring1.setMother( a );

		Animal offspring2 = new Animal();
		offspring2.setDescription( "offspring2" );
		a.addOffspring( offspring2 );
		offspring2.setMother( a );

		scope.inTransaction(
				session -> {
					session.persist( mother );
					session.persist( a );
					session.persist( offspring1 );
					session.persist( offspring2 );
				}
		);


		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getCache().evictQueryRegions();
		sessionFactory.getStatistics().clear();

		scope.inTransaction(
				session -> {

					List list = session.createQuery( "from Animal a left join fetch a.mother" ).setCacheable( true )
							.list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 0 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 1 );
					list = session.createQuery( "select a from Animal a left join fetch a.mother" ).setCacheable( true )
							.list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 1 );
					list = session.createQuery( "select a, m from Animal a left join a.mother m" ).setCacheable( true )
							.list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testCachedJoinedAndJoinFetchedOneToMany(SessionFactoryScope scope) throws Exception {
		Animal a = new Animal();
		a.setDescription( "an animal" );
		Animal mother = new Animal();
		mother.setDescription( "a mother" );
		mother.addOffspring( a );
		a.setMother( mother );
		Animal offspring1 = new Animal();
		offspring1.setDescription( "offspring1" );
		Animal offspring2 = new Animal();
		offspring1.setDescription( "offspring2" );
		a.addOffspring( offspring1 );
		offspring1.setMother( a );
		a.addOffspring( offspring2 );
		offspring2.setMother( a );

		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getCache().evictQueryRegions();
		sessionFactory.getStatistics().clear();

		scope.inTransaction(
				session -> {
					session.persist( mother );
					session.persist( a );
					session.persist( offspring1 );
					session.persist( offspring2 );
				}
		);


		scope.inTransaction(
				session -> {
					List list = session.createQuery( "from Animal a left join fetch a.offspring" ).setCacheable( true )
							.list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 0 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 1 );
					list = session.createQuery( "select a from Animal a left join fetch a.offspring" )
							.setCacheable( true ).list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 1 );
					list = session.createQuery( "select a, o from Animal a left join a.offspring o" )
							.setCacheable( true ).list();
					assertThat( sessionFactory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
					assertThat( sessionFactory.getStatistics().getQueryCachePutCount() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testSelectNewTransformerQueries(SessionFactoryScope scope) {
		createTestBaseData( scope );
		scope.inTransaction(
				session -> {
					List list = session.createQuery(
									"select new Animal(an.description, an.bodyWeight) as animal from Animal an order by an.description" )
							.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP )
							.list();
					assertThat( list.size() ).isEqualTo( 2 );
					Map<String, Animal> m1 = (Map<String, Animal>) list.get( 0 );
					Map<String, Animal> m2 = (Map<String, Animal>) list.get( 1 );
					assertThat( m1.size() ).isEqualTo( 1 );
					assertThat( m2.size() ).isEqualTo( 1 );
					assertThat( m1.get( "animal" ).getDescription() ).isEqualTo( "Mammal #1" );
					assertThat( m2.get( "animal" ).getDescription() ).isEqualTo( "Mammal #2" );
				}
		);
		destroyTestBaseData( scope );
	}

	@Test
	public void testResultTransformerScalarQueries(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		String query = "select an.description as description, an.bodyWeight as bodyWeight from Animal an order by bodyWeight desc";

		scope.inTransaction(
				session -> {
					List results = session.createQuery( query )
							.setResultTransformer( Transformers.aliasToBean( Animal.class ) ).list();

					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( Animal.class );
					Animal firstAnimal = (Animal) results.get( 0 );
					Animal secondAnimal = (Animal) results.get( 1 );
					assertThat( firstAnimal.getDescription() ).isEqualTo( "Mammal #1" );
					assertThat( secondAnimal.getDescription() ).isEqualTo( "Mammal #2" );
					assertThat( session.contains( firstAnimal ) ).isFalse();
				}
		);

		scope.inTransaction(
				session -> {
					try (ScrollableResults sr = session.createQuery( query )
							.setResultTransformer( Transformers.aliasToBean( Animal.class ) ).scroll()) {
						assertThat( sr.next() ).as( "Incorrect result size" ).isTrue();
						assertThat( sr.get() ).as( "Incorrect return type" ).isInstanceOf( Animal.class );
						assertThat( session.contains( sr.get() ) ).isFalse();
					}
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "select a from Animal a, Animal b order by a.id" )
							.setResultTransformer( new ResultTransformer() {
								@Override
								public Object transformTuple(Object[] tuple, String[] aliases) {
									return tuple[0];
								}
							} )
							.list();
					assertThat( results.size() ).as( "Incorrect result size" ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).as( "Incorrect return type" ).isInstanceOf( Animal.class );
					Animal firstAnimal = (Animal) results.get( 0 );
					Animal secondAnimal = (Animal) results.get( 1 );
					assertThat( firstAnimal.getDescription() ).isEqualTo( "Mammal #1" );
					assertThat( secondAnimal.getDescription() ).isEqualTo( "Mammal #2" );
				}
		);

		destroyTestBaseData( scope );
	}

	@Test
	public void testResultTransformerEntityQueries(SessionFactoryScope scope) throws Exception {
		createTestBaseData( scope );

		String query = "select an as an from Animal an order by bodyWeight desc";

		scope.inTransaction(
				session -> {
					List results = session.createQuery( query )
							.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP ).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isInstanceOf( Map.class );
					Map map = ((Map) results.get( 0 ));
					assertThat( map.size() ).isEqualTo( 1 );
					Animal firstAnimal = (Animal) map.get( "an" );
					map = ((Map) results.get( 1 ));
					Animal secondAnimal = (Animal) map.get( "an" );
					assertThat( firstAnimal.getDescription() ).isEqualTo( "Mammal #1" );
					assertThat( secondAnimal.getDescription() ).isEqualTo( "Mammal #2" );
					assertThat( session.contains( firstAnimal ) ).isTrue();
					assertThat( session.get( Animal.class, firstAnimal.getId() ) ).isSameAs( firstAnimal );

				}
		);

		scope.inTransaction(
				session -> {
					try (ScrollableResults sr = session.createQuery( query )
							.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP ).scroll()) {
						assertThat( sr.next() ).as( "Incorrect result size" ).isTrue();
						assertThat( sr.get() ).isInstanceOf( Map.class );
					}
				}
		);

		destroyTestBaseData( scope );
	}

	@Test
	public void testEJBQLFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String hql = "from Animal a where a.description = concat('1', concat('2','3'), '4'||'5')||'0'";
					session.createQuery( hql ).list();

					hql = "from Animal a where substring(a.description, 1, 3) = 'cat'";
					session.createQuery( hql ).list();

					hql = "select substring(a.description, 1, 3) from Animal a";
					session.createQuery( hql ).list();

					hql = "from Animal a where lower(a.description) = 'cat'";
					session.createQuery( hql ).list();

					hql = "select lower(a.description) from Animal a";
					session.createQuery( hql ).list();

					hql = "from Animal a where upper(a.description) = 'CAT'";
					session.createQuery( hql ).list();

					hql = "select upper(a.description) from Animal a";
					session.createQuery( hql ).list();

					hql = "from Animal a where length(a.description) = 5";
					session.createQuery( hql ).list();

					hql = "select length(a.description) from Animal a";
					session.createQuery( hql ).list();

					Dialect dialect = session.getDialect();
					// Informix before version 12 didn't support finding the index of substrings
					if ( !(dialect instanceof InformixDialect && dialect.getVersion().isBefore( 12 )) ) {
						//note: postgres and db2 don't have a 3-arg form, it gets transformed to 2-args
						hql = "from Animal a where locate('abc', a.description, 2) = 2";
						session.createQuery( hql ).list();

						hql = "from Animal a where locate('abc', a.description) = 2";
						session.createQuery( hql ).list();

						hql = "select locate('cat', a.description, 2) from Animal a";
						session.createQuery( hql ).list();
					}

					if ( !(dialect instanceof DB2Dialect) ) {
						hql = "from Animal a where trim(trailing '_' from a.description) = 'cat'";
						session.createQuery( hql ).list();

						hql = "select trim(trailing '_' from a.description) from Animal a";
						session.createQuery( hql ).list();

						hql = "from Animal a where trim(leading '_' from a.description) = 'cat'";
						session.createQuery( hql ).list();

						hql = "from Animal a where trim(both from a.description) = 'cat'";
						session.createQuery( hql ).list();
					}

					if ( !(dialect instanceof HSQLDialect) ) { //HSQL doesn't like trim() without specification
						hql = "from Animal a where trim(a.description) = 'cat'";
						session.createQuery( hql ).list();
					}

					hql = "from Animal a where abs(a.bodyWeight) = sqrt(a.bodyWeight)";
					session.createQuery( hql ).list();

					hql = "from Animal a where mod(16, 4) = 4";
					session.createQuery( hql ).list();

					hql = "from Animal a where bit_length(str(a.bodyWeight)) = 24";
					session.createQuery( hql ).list();

					hql = "select bit_length(str(a.bodyWeight)) from Animal a";
					session.createQuery( hql ).list();

					/*hql = "select object(a) from Animal a where CURRENT_DATE = :p1 or CURRENT_TIME = :p2 or CURRENT_TIMESTAMP = :p3";
					session.createQuery(hql).list();*/

					// todo the following is not supported
					//hql = "select CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP from Animal a";
					//parse(hql, true);
					//System.out.println("sql: " + toSql(hql));

					hql = "from Animal a where a.description like '%a%'";
					session.createQuery( hql ).list();

					hql = "from Animal a where a.description not like '%a%'";
					session.createQuery( hql ).list();

					hql = "from Animal a where a.description like 'x%ax%' escape 'x'";
					session.createQuery( hql ).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11942")
	public void testOrderByExtraParenthesis(SessionFactoryScope scope) throws Exception {
		try {
			scope.inTransaction( session -> {
				session.createQuery(
								"select a from Product a " +
								"where " +
								"coalesce(a.description, :description) = :description ) " +
								"order by a.description ", Product.class )
						.setParameter( "description", "desc" )
						.getResultList();
				fail( "Should have thrown exception" );
			} );
		}
		catch (IllegalArgumentException e) {
			final Throwable cause = e.getCause();
			assertThat( cause ).isInstanceOf( SyntaxException.class );
			assertThat( cause.getMessage() ).contains( "mismatched input ')'" );
		}
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportSubqueryAsLeftHandSideInPredicate.class,
			comment = "Database does not support using subquery as singular value expression"
	)
	public void testSubqueryAsSingularValueExpression(SessionFactoryScope scope) {
		assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) in (1,2,3)", 0, scope );
		assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) between 0 and 100", 0, scope );
		assertResultSize( "from Animal x where (select max(a.description) from Animal a) like 'big%'", 0, scope );
		assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) is not null", 0, scope );
	}

	public void testExistsSubquery(SessionFactoryScope scope) {
		assertResultSize( "from Animal x where exists (select max(a.bodyWeight) from Animal a)", 0, scope );
	}

	private void assertResultSize(String hql, int size, SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertThat( session.createQuery( hql ).list().size() ).isEqualTo( size )

		);
	}

	private interface QueryPreparer {
		void prepare(Query query);
	}

	private static final QueryPreparer DEFAULT_PREPARER = new QueryPreparer() {
		public void prepare(Query query) {
		}
	};

	private class SyntaxChecker {
		private final String hql;
		private final QueryPreparer preparer;

		public SyntaxChecker(String hql) {
			this( hql, DEFAULT_PREPARER );
		}

		public SyntaxChecker(String hql, QueryPreparer preparer) {
			this.hql = hql;
			this.preparer = preparer;
		}

		public void checkAll(SessionFactoryScope scope) {
			checkList( scope );
			checkScroll( scope );
		}

		public SyntaxChecker checkList(SessionFactoryScope scope) {
			scope.inTransaction(
					session -> {
						Query query = session.createQuery( hql, Object[].class );
						preparer.prepare( query );
						query.list();
					}

			);
			return this;
		}

		public SyntaxChecker checkScroll(SessionFactoryScope scope) {
			scope.inTransaction(
					session -> {
						Query query = session.createQuery( hql, Object[].class );
						preparer.prepare( query );
						query.scroll().close();
					}
			);
			return this;
		}
	}
}
