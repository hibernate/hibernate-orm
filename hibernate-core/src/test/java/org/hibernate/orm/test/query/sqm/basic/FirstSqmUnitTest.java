/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.basic;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.tree.SqmSelectStatement;


import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class FirstSqmUnitTest extends BaseSqmUnitTest {

	// todo (6.0) : this test can likely just go away ultimately.
	//		it was intended just as a simple first "smoke" test

	@Entity( name = "Person" )
	public static class Person {
		@Id
		public Integer id;
		public String name;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
	}

	@Test
	public void testSelectId() {
		final SqmSelectStatement sqm = interpretSelect( "select p.id from Person p" );

		assertThat( sqm, notNullValue() );
		assertThat(
				sqm.getQuerySpec().getFromClause().getFromElementSpaces().size(),
				is( 1 )
		);
		assertThat(
				sqm.getQuerySpec().getFromClause().getFromElementSpaces().get( 0 ).getRoot().getEntityName(),
				is( getSessionFactory().getMetamodel().findEntityDescriptor( Person.class ).getEntityName() )
		);
	}
}
