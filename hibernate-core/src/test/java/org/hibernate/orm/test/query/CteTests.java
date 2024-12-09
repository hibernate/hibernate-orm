/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaJoinedFrom;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Address;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory
public class CteTests {

	@Test
	public void testBasic(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cte = cb.createTupleQuery();
					final JpaRoot<Contact> cteRoot = cte.from( Contact.class );
					cte.multiselect( cteRoot.get( "id" ).alias( "id" ), cteRoot.get( "name" ).alias( "name" ) );
					cte.where( cb.equal( cteRoot.get( "gender" ), Contact.Gender.FEMALE ) );

					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaCteCriteria<Tuple> femaleContacts = cq.with( cte );

					final JpaRoot<Tuple> root = cq.from( femaleContacts );

					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.orderBy( cb.asc( root.get( "id" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with femaleContacts as (" +
									"select c.id id, c.name name from Contact c where c.gender = FEMALE" +
									")" +
									"select c.id, c.name from femaleContacts c order by c.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17897")
	public void testBasicJoined(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cte = cb.createTupleQuery();
					final JpaRoot<Contact> cteRoot = cte.from( Contact.class );
					cte.multiselect( cteRoot.get( "id" ).alias( "id" ), cteRoot.get( "name" ).alias( "name" ) );
					cte.where( cb.equal( cteRoot.get( "gender" ), Contact.Gender.FEMALE ) );

					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaCteCriteria<Tuple> femaleContacts = cq.with( cte );

					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaJoinedFrom<?, Tuple> join = root.join( femaleContacts );
					join.on( root.get( "id" ).equalTo( join.get( "id" ) ) );

					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.orderBy( cb.asc( root.get( "id" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with femaleContacts as (" +
									"select c.id id, c.name name from Contact c where c.gender = FEMALE" +
									")" +
									"select c.id, c.name from Contact c join femaleContacts f on c.id = f.id order by c.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	public void testNested(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> allContactsQuery = cb.createTupleQuery();
					{
						final JpaRoot<Contact> allContactsRoot = allContactsQuery.from( Contact.class );
						allContactsQuery.multiselect(
								allContactsRoot.get( "id" ).alias( "id" ),
								allContactsRoot.get( "name" ).alias( "name" ),
								allContactsRoot.get( "gender" ).alias( "gender" )
						);
					}

					final JpaCriteriaQuery<Tuple> femaleContactsQuery = cb.createTupleQuery();
					{
						final JpaCteCriteria<Tuple> allContacts = femaleContactsQuery.with( allContactsQuery );
						final JpaRoot<Tuple> femaleContactsRoot = femaleContactsQuery.from( allContacts );
						femaleContactsQuery.multiselect(
								femaleContactsRoot.get( "id" ).alias( "id" ),
								femaleContactsRoot.get( "name" ).alias( "name" )
						);
						femaleContactsQuery.where( cb.equal(
								femaleContactsRoot.get( "gender" ),
								Contact.Gender.FEMALE
						) );
					}

					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaCteCriteria<Tuple> femaleContacts = cq.with( femaleContactsQuery );

					final JpaRoot<Tuple> root = cq.from( femaleContacts );

					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.orderBy( cb.asc( root.get( "id" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with femaleContacts as (" +
									"with allContacts as (" +
									"select c.id id, c.name name, c.gender gender from Contact c" +
									")" +
									"select c.id id, c.name name from allContacts c where c.gender = FEMALE" +
									")" +
									"select c.id, c.name from femaleContacts c order by c.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "The emulation of CTEs in subqueries results in correlation in nesting level 2, which is not possible with Sybase ASE")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "The TiDB version on CI seems to be buggy")
	public void testSubquery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.orderBy( cb.asc( root.get( "id" ) ) );

					final JpaSubQuery<String> subquery = cq.subquery( String.class );

					final JpaSubQuery<Tuple> addressesQuery = cq.subquery( Tuple.class );
					{
						final JpaJoin<?, Address> addresses = addressesQuery.correlate( root ).join( "addresses" );
						addressesQuery.multiselect(
								addresses.get( "line1" ).alias( "line" )
						);
					}
					final JpaCteCriteria<Tuple> addresses = subquery.with( addressesQuery );
					final JpaRoot<Tuple> addressesRoot = subquery.from( addresses );
					subquery.select( addressesRoot.get( "line" ) );
					cq.where( cb.exists( subquery ) );

					final QueryImplementor<Tuple> query = session.createQuery(
									"select c.id, c.name from Contact c where exists (" +
									"with addresses as (" +
									"select a.line1 line from c.addresses a" +
									")" +
									"select a.line from addresses a" +
									") order by c.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "The emulation of CTEs in subqueries results in correlation in nesting level 2, which is not possible with Sybase ASE")
	public void testInSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<String> cq = cb.createQuery( String.class );
			final JpaRoot<Contact> root = cq.from( Contact.class );
			final JpaSubQuery<Tuple> cteQuery = cq.subquery( Tuple.class );
			final JpaRoot<Contact> cteRoot = cteQuery.from( Contact.class );
			cteQuery.multiselect(
					cteRoot.get( "id" ).alias( "id" ),
					cteRoot.get( "name" ).get( "first" ).alias( "firstName" )
			).where(
					cteRoot.get( "id" ).in( 1, 2 )
			);
			final JpaSubQuery<Integer> subquery = cq.subquery( Integer.class );
			final JpaCteCriteria<Tuple> cte = subquery.with( cteQuery );
			final JpaRoot<Tuple> sqRoot = subquery.from( cte );
			subquery.select( sqRoot.get( "id" ) );
			cq.select( root.get( "name" ).get( "first" ) ).where( root.get( "id" ).in( subquery ) );

			final QueryImplementor<String> query = session.createQuery(
					"select c.name.first from Contact c where c.id in (" +
							"with cte as (" +
							"select c.id id, c.name.first firstName from Contact c " +
							"where c.id in (1,2)" +
							") " +
							"select c.id from cte c" +
							")",
					String.class
			);

			verifySame(
					session.createQuery( cq ).getResultList(),
					query.getResultList(),
					list -> {
						assertEquals( 2, list.size() );
						assertThat( list ).containsOnly( "John", "Jane" );
					}
			);
		} );
	}

	@Test
	public void testMaterialized(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cte = cb.createTupleQuery();
					final JpaRoot<Contact> cteRoot = cte.from( Contact.class );
					cte.multiselect( cteRoot.get( "id" ).alias( "id" ), cteRoot.get( "name" ).alias( "name" ) );
					cte.where( cb.equal( cteRoot.get( "gender" ), Contact.Gender.FEMALE ) );

					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaCteCriteria<Tuple> femaleContacts = cq.with( cte );
					femaleContacts.setMaterialization( CteMaterialization.MATERIALIZED );

					final JpaRoot<Tuple> root = cq.from( femaleContacts );

					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.orderBy( cb.asc( root.get( "id" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with femaleContacts as materialized (" +
									"select c.id id, c.name name from Contact c where c.gender = FEMALE" +
									")" +
									"select c.id, c.name from femaleContacts c order by c.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void testSimpleRecursive(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaParameterExpression<Integer> param = cb.parameter( Integer.class );
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();

					final JpaCriteriaQuery<Tuple> baseQuery = cb.createTupleQuery();
					final JpaRoot<Contact> baseRoot = baseQuery.from( Contact.class );
					baseQuery.multiselect( baseRoot.get( "alternativeContact" ).alias( "alt" ) );
					baseQuery.where( cb.equal( baseRoot.get( "id" ), param ) );

					final JpaCteCriteria<Tuple> alternativeContacts = cq.withRecursiveUnionAll(
							baseQuery,
							selfType -> {
								final JpaCriteriaQuery<Tuple> recursiveQuery = cb.createTupleQuery();
								final JpaRoot<Tuple> recursiveRoot = recursiveQuery.from( selfType );
								recursiveQuery.multiselect( recursiveRoot.get( "alt" ).get( "alternativeContact" ).alias( "alt" ) );
								recursiveQuery.where( cb.notEqual( recursiveRoot.get( "alt" ).get( "alternativeContact" ).get( "id" ), param ) );
								return recursiveQuery;
							}
					);

					final JpaRoot<Tuple> root = cq.from( alternativeContacts );
					final JpaJoin<Object, Object> alt = root.join( "alt" );
					cq.multiselect( alt );
					cq.orderBy( cb.asc( alt.get( "id" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with alternativeContacts as (" +
									"select c.alternativeContact alt from Contact c where c.id = :param " +
									"union all " +
									"select c.alt.alternativeContact alt from alternativeContacts c where c.alt.alternativeContact.id <> :param" +
									")" +
									"select ac from alternativeContacts c join c.alt ac order by ac.id",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).setParameter( param, 1 ).getResultList(),
							query.setParameter( "param", 1 ).getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "Jane", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void testRecursiveCycleClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaParameterExpression<Integer> param = cb.parameter( Integer.class );
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();

					final JpaCriteriaQuery<Tuple> baseQuery = cb.createTupleQuery();
					final JpaRoot<Contact> baseRoot = baseQuery.from( Contact.class );
					baseQuery.multiselect( baseRoot.get( "alternativeContact" ).alias( "alt" ) );
					baseQuery.where( cb.equal( baseRoot.get( "id" ), param ) );

					final JpaCteCriteria<Tuple> alternativeContacts = cq.withRecursiveUnionAll(
							baseQuery,
							selfType -> {
								final JpaCriteriaQuery<Tuple> recursiveQuery = cb.createTupleQuery();
								final JpaRoot<Tuple> recursiveRoot = recursiveQuery.from( selfType );
								recursiveQuery.multiselect( recursiveRoot.get( "alt" ).get( "alternativeContact" ).alias( "alt" ) );
								return recursiveQuery;
							}
					);
					alternativeContacts.cycle(
							"isCycle",
							true,
							false,
							alternativeContacts.getType().getAttribute( "alt" )
					);

					final JpaRoot<Tuple> root = cq.from( alternativeContacts );
					final JpaJoin<Object, Object> alt = root.join( "alt" );
					cq.multiselect( alt, root.get( "isCycle" ) );
					cq.orderBy( cb.asc( alt.get( "id" ) ), cb.asc( root.get( "isCycle" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with alternativeContacts as (" +
									"select c.alternativeContact alt from Contact c where c.id = :param " +
									"union all " +
									"select c.alt.alternativeContact alt from alternativeContacts c" +
									")" +
									"cycle alt set isCycle to true default false " +
									"select ac, c.isCycle from alternativeContacts c join c.alt ac order by ac.id, c.isCycle",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).setParameter( param, 1 ).getResultList(),
							query.setParameter( "param", 1 ).getResultList(),
							list -> {
								assertEquals( 4, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "Jane", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
								assertFalse( list.get( 1 ).get( 1, Boolean.class ) );
								assertEquals( "Jane", list.get( 2 ).get( 0, Contact.class ).getName().getFirst() );
								assertTrue( list.get( 2 ).get( 1, Boolean.class ) );
								assertEquals( "Granny", list.get( 3 ).get( 0, Contact.class ).getName().getFirst() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void testRecursiveCycleUsingClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaParameterExpression<Integer> param = cb.parameter( Integer.class );
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();

					final JpaCriteriaQuery<Tuple> baseQuery = cb.createTupleQuery();
					final JpaRoot<Contact> baseRoot = baseQuery.from( Contact.class );
					baseQuery.multiselect( baseRoot.get( "alternativeContact" ).alias( "alt" ) );
					baseQuery.where( cb.equal( baseRoot.get( "id" ), param ) );

					final JpaCteCriteria<Tuple> alternativeContacts = cq.withRecursiveUnionAll(
							baseQuery,
							selfType -> {
								final JpaCriteriaQuery<Tuple> recursiveQuery = cb.createTupleQuery();
								final JpaRoot<Tuple> recursiveRoot = recursiveQuery.from( selfType );
								recursiveQuery.multiselect( recursiveRoot.get( "alt" ).get( "alternativeContact" ).alias( "alt" ) );
								return recursiveQuery;
							}
					);
					alternativeContacts.cycleUsing(
							"isCycle",
							"path",
							true,
							false,
							alternativeContacts.getType().getAttribute( "alt" )
					);

					final JpaRoot<Tuple> root = cq.from( alternativeContacts );
					final JpaJoin<Object, Object> alt = root.join( "alt" );
					cq.multiselect( alt, root.get( "isCycle" ) );
					cq.orderBy( cb.asc( alt.get( "id" ) ), cb.asc( root.get( "isCycle" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"with alternativeContacts as (" +
									"select c.alternativeContact alt from Contact c where c.id = :param " +
									"union all " +
									"select c.alt.alternativeContact alt from alternativeContacts c" +
									")" +
									"cycle alt set isCycle to true default false using path " +
									"select ac, c.isCycle from alternativeContacts c join c.alt ac order by ac.id, c.isCycle",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).setParameter( param, 1 ).getResultList(),
							query.setParameter( "param", 1 ).getResultList(),
							list -> {
								assertEquals( 4, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "Jane", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
								assertFalse( list.get( 1 ).get( 1, Boolean.class ) );
								assertEquals( "Jane", list.get( 2 ).get( 0, Contact.class ).getName().getFirst() );
								assertTrue( list.get( 2 ).get( 1, Boolean.class ) );
								assertEquals( "Granny", list.get( 3 ).get( 0, Contact.class ).getName().getFirst() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void testRecursiveSearchClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					//noinspection unchecked
					final JpaParameterExpression<List<Integer>> param = cb.parameter( (Class<List<Integer>>) (Class<?>) List.class );
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();

					final JpaCriteriaQuery<Tuple> baseQuery = cb.createTupleQuery();
					final JpaRoot<Contact> baseRoot = baseQuery.from( Contact.class );
					baseQuery.multiselect(
							baseRoot.get( "id" ).alias( "id" ),
							baseRoot.get( "alternativeContact" ).get( "id" ).alias( "altId" ),
							cb.literal( 1 ).alias( "depth" )
					);
					baseQuery.where( cb.in( baseRoot.get( "id" ), param ) );

					final JpaCteCriteria<Tuple> alternativeContacts = cq.withRecursiveUnionAll(
							baseQuery,
							selfType -> {
								final JpaCriteriaQuery<Tuple> recursiveQuery = cb.createTupleQuery();
								final JpaRoot<Tuple> recursiveRoot = recursiveQuery.from( selfType );
								final JpaEntityJoin<Contact> contact = recursiveRoot.join( Contact.class );
								contact.on( cb.equal( recursiveRoot.get( "altId" ), contact.get( "id" ) ) );
								recursiveQuery.multiselect(
										contact.get( "id" ).alias( "id" ),
										contact.get( "alternativeContact" ).get( "id" ).alias( "altId" ),
										cb.sum( recursiveRoot.get( "depth" ), cb.literal( 1 ) ).alias( "depth" )
								);
								return recursiveQuery;
							}
					);
					alternativeContacts.search(
							CteSearchClauseKind.BREADTH_FIRST,
							"orderAttr",
							cb.search( alternativeContacts.getType().getAttribute( "id" ) )
					);

					final JpaRoot<Tuple> root = cq.from( alternativeContacts );
					final JpaEntityJoin<Contact> alt = root.join( Contact.class );
					alt.on( cb.equal( root.get( "id" ), alt.get( "id" ) ) );
					cq.multiselect( alt );
					cq.orderBy( cb.asc( root.get( "orderAttr" ) ) );
					cq.fetch( 4 );

					final QueryImplementor<Tuple> breadthFirstQuery = session.createQuery(
							"with alternativeContacts as (" +
									"select c.id id, c.alternativeContact.id altId, 1 depth from Contact c where c.id in :param " +
									"union all " +
									"select c.id id, c.alternativeContact.id altId, ac.depth + 1 depth from alternativeContacts ac join Contact c on ac.altId = c.id" +
									") search breadth first by id set orderAttr " +
									"select c from alternativeContacts ac join Contact c on ac.id = c.id order by ac.orderAttr limit 4",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).setParameter( param, List.of( 4, 7 ) ).getResultList(),
							breadthFirstQuery.setParameter( "param", List.of( 4, 7 ) ).getResultList(),
							list -> {
								assertEquals( 4, list.size() );
								assertEquals( "C4", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C7", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C5", list.get( 2 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C8", list.get( 3 ).get( 0, Contact.class ).getName().getFirst() );
							}
					);

					final QueryImplementor<Tuple> depthFirstQuery = session.createQuery(
							"with alternativeContacts as (" +
									"select c.id id, c.alternativeContact.id altId, 1 depth from Contact c where c.id in :param " +
									"union all " +
									"select c.id id, c.alternativeContact.id altId, ac.depth + 1 depth from alternativeContacts ac join Contact c on ac.altId = c.id" +
									") search depth first by id set orderAttr " +
									"select ac from alternativeContacts c join Contact ac on c.id = ac.id order by c.orderAttr limit 4",
							Tuple.class
					);
					alternativeContacts.search(
							CteSearchClauseKind.DEPTH_FIRST,
							"orderAttr",
							cb.search( alternativeContacts.getType().getAttribute( "id" ) )
					);
					verifySame(
							session.createQuery( cq ).setParameter( param, List.of( 4, 7 ) ).getResultList(),
							depthFirstQuery.setParameter( "param", List.of( 4, 7 ) ).getResultList(),
							list -> {
								assertEquals( 4, list.size() );
								assertEquals( "C4", list.get( 0 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C5", list.get( 1 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C6", list.get( 2 ).get( 0, Contact.class ).getName().getFirst() );
								assertEquals( "C7", list.get( 3 ).get( 0, Contact.class ).getName().getFirst() );
							}
					);
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = new Contact(
					1,
					new Contact.Name( "John", "Doe" ),
					Contact.Gender.MALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact = new Contact(
					2,
					new Contact.Name( "Jane", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact2 = new Contact(
					3,
					new Contact.Name( "Granny", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			alternativeContact.setAlternativeContact( alternativeContact2 );
			contact.setAlternativeContact( alternativeContact );
			contact.setAddresses(
					List.of(
							new Address( "Street 1", 1234 ),
							new Address( "Street 2", 5678 )
					)
			);
			session.persist( alternativeContact2 );
			session.persist( alternativeContact );
			session.persist( contact );
			alternativeContact2.setAlternativeContact( contact );

			final Contact c4 = new Contact(
					4,
					new Contact.Name( "C4", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c5 = new Contact(
					5,
					new Contact.Name( "C5", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c6 = new Contact(
					6,
					new Contact.Name( "C6", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c7 = new Contact(
					7,
					new Contact.Name( "C7", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c8 = new Contact(
					8,
					new Contact.Name( "C8", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			c4.setAlternativeContact( c5 );
			c5.setAlternativeContact( c6 );
			c7.setAlternativeContact( c8 );

			session.persist( c6 );
			session.persist( c5 );
			session.persist( c4 );
			session.persist( c8 );
			session.persist( c7 );
		} );
	}

	private <T> void verifySame(T criteriaResult, T hqlResult, Consumer<T> verifier) {
		verifier.accept( criteriaResult );
		verifier.accept( hqlResult );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "update Contact set alternativeContact = null" ).executeUpdate();
			session.createMutationQuery( "delete Contact" ).executeUpdate();
		} );
	}
}
