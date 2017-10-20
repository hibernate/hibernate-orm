/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.time.Instant;
import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseUnitTest;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class AttributePathTests extends BaseUnitTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
	}

	@Test
	public void testImplicitJoinReuse() {
		final SqmSelectStatement statement = interpretSelect( "select s.mate.name.first, s.mate.name.last from Person s" );

		assertThat( statement.getQuerySpec().getFromClause().getFromElementSpaces().size(), is(1) );
		final SqmFromElementSpace space = statement.getQuerySpec().getFromClause().getFromElementSpaces().get( 0 );

		assertThat( space.getJoins().size(), is(1) );

		// from-clause paths
//		assertPropertyPath( space.getRoot(), "com.acme.Something(s)" );
//		assertPropertyPath( space.getJoins().get( 0 ), "com.acme.Something(s).entity" );

		final List<SqmSelection> selections = statement.getQuerySpec().getSelectClause().getSelections();
		assertThat( selections.size(), is(2) );

		// expression paths
		assertPropertyPath( (SqmExpression) selections.get( 0 ).getSelectableNode(), Person.class.getName() + "(s).mate.name.first" );
		assertPropertyPath( (SqmExpression) selections.get( 1 ).getSelectableNode(), Person.class.getName() + "(s).mate.name.last" );
	}

	private void assertPropertyPath(SqmExpression expression, String expectedFullPath) {
		assertThat( expression, instanceOf( SqmNavigableReference.class ) );
		final SqmNavigableReference domainReferenceBinding = (SqmNavigableReference) expression;
		assertThat( domainReferenceBinding.getNavigablePath().getFullPath(), is( expectedFullPath) );
	}

	@Test
	public void testImplicitJoinReuse2() {
		final SqmSelectStatement statement = interpretSelect( "select s.mate from Person s where s.mate.name.first = ?1" );

		assertThat( statement.getQuerySpec().getFromClause().getFromElementSpaces().size(), is(1) );
		final SqmFromElementSpace space = statement.getQuerySpec().getFromClause().getFromElementSpaces().get( 0 );

		assertThat( space.getJoins().size(), is(1) );

		final SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getSelectableNode(), instanceOf( SqmSingularAttributeReference.class ) );
		final SqmSingularAttributeReference selectExpression = (SqmSingularAttributeReference) selection.getSelectableNode();
		assertThat( selectExpression.getExportedFromElement(), notNullValue() );

		final RelationalSqmPredicate predicate = (RelationalSqmPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		final SqmSingularAttributeReference predicateLhs = (SqmSingularAttributeReference) predicate.getLeftHandExpression();
		assertThat( predicateLhs.getSourceReference().getExportedFromElement(), notNullValue() );


		// from-clause paths
//		assertPropertyPath( space.getRoot(), "com.acme.Something(s)" );
//		assertPropertyPath( space.getJoins().get( 0 ), "com.acme.Something(s).entity" );

		// expression paths
		assertPropertyPath( (SqmExpression) selection.getSelectableNode(), Person.class.getName() + "(s).mate" );
		assertPropertyPath( predicateLhs, Person.class.getName() + "(s).mate.name.first" );
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
		public Name name;

		public String nickName;

		@ManyToOne
		Person mate;

		@Temporal( TemporalType.TIMESTAMP )
		public Instant dob;

		public int numberOfToes;
	}

}
