/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.time.Instant;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Initial work on a "from clause processor"
 *
 * @author Steve Ebersole
 */
public class FromClauseTests extends BaseSqmUnitTest {
	@Test
	public void testSimpleFrom() {
		final SqmSelectStatement selectStatement = interpretSelect( "select p.nickName from Person p" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );

		final SqmRoot firstSpaceRoot = firstSpace.getRoot();
		assertThat( firstSpaceRoot, notNullValue() );
		assertThat( firstSpaceRoot.getIdentificationVariable(), is( "p") );
	}

	@Test
	public void testMultipleSpaces() {
		final SqmSelectStatement selectStatement = interpretSelect(
				"select p.nickName from Person p, Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();

		assertNotNull( fromClause );
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 2 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );
		assertThat( firstSpace.getRoot(), notNullValue() );
		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( "p")  );

		final SqmFromElementSpace secondSpace = fromClause.getFromElementSpaces().get( 1 );
		assertThat( secondSpace, notNullValue() );
		assertThat( secondSpace.getJoins(), isEmpty() );
		assertThat( secondSpace.getRoot(), notNullValue() );
		assertThat( secondSpace.getRoot().getIdentificationVariable(), is( "p2")  );
	}

	@Test
	public void testImplicitAlias() {
		final SqmSelectStatement selectStatement = interpretSelect( "select nickName from Person" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );
		assertThat( firstSpace.getRoot(), notNullValue() );
		assertTrue( ImplicitAliasGenerator.isImplicitAlias( firstSpace.getRoot().getIdentificationVariable() ) );
	}

	@Test
	public void testCrossJoin() {
		final SqmSelectStatement selectStatement = interpretSelect(
				"select p.nickName from Person p cross join Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), hasSize( 1 ) );
		assertThat( firstSpace.getRoot(), notNullValue() );

		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( "p" )  );
		assertThat( firstSpace.getJoins().get( 0 ).getIdentificationVariable(), is( "p2" )  );
	}

	@Test
	public void testSimpleImplicitInnerJoin() {
		simpleJoinAssertions(
				interpretSelect( "select p.nickName from Person p join p.mate m" ),
				SqmJoinType.INNER,
				"p",
				"m"
		);
	}

	private void simpleJoinAssertions(
			SqmSelectStatement selectStatement,
			SqmJoinType joinType,
			String rootAlias,
			String joinAlias) {
		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );

		assertThat( firstSpace.getRoot(), notNullValue() );
		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( rootAlias )  );

		assertThat( firstSpace.getJoins(), hasSize( 1 ) );
		assertThat( firstSpace.getJoins().get( 0 ).getIdentificationVariable(), is( joinAlias )  );
		assertThat( firstSpace.getJoins().get( 0 ).getJoinType(), is( joinType ) );
	}

	@Test
	public void testSimpleExplicitInnerJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a inner join a.mate c" ),
				SqmJoinType.INNER,
				"a",
				"c"
		);
	}

	@Test
	public void testSimpleExplicitOuterJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a outer join a.mate c" ),
				SqmJoinType.LEFT,
				"a",
				"c"
		);
	}

	@Test
	public void testSimpleExplicitLeftOuterJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a left outer join a.mate c" ),
				SqmJoinType.LEFT,
				"a",
				"c"
		);
	}

	@Test
	public void testAttributeJoinWithOnClause() {
		final SqmSelectStatement selectStatement = interpretSelect(
				"select a from Person a left outer join a.mate c on c.numberOfToes > 5 and c.numberOfToes < 20 "
		);

		simpleJoinAssertions(
				selectStatement,
				SqmJoinType.LEFT,
				"a",
				"c"
		);

		final SqmJoin join = selectStatement.getQuerySpec()
				.getFromClause()
				.getFromElementSpaces()
				.get( 0 )
				.getJoins()
				.get( 0 );

		// todo (6.0) : check join restrictions
		//		not yet tracked, nor exposed.  SqmPredicate
	}


	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
	}

	@Entity( name = "Person" )
	public static class Person {
		@Embeddable
		public static class Name {
			public String first;
			public String last;
		}

		@Id
		public Integer pk;

		@Embedded
		public Person.Name name;

		public String nickName;

		@ManyToOne
		Person mate;

		@Temporal( TemporalType.TIMESTAMP )
		public Instant dob;

		public int numberOfToes;
	}
}
