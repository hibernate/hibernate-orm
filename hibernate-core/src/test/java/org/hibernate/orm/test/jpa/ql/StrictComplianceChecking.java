/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.ql;

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
import org.hibernate.query.sqm.StrictJpaComplianceViolation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Steve Ebersole
 */
public class StrictComplianceChecking extends BaseSqmUnitTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCollectionValueFunctionNotSupportedInStrictMode() {
		expectedException.expect( StrictJpaComplianceViolation.class );
		expectedException.expectMessage( "Encountered application of value() function to path expression which does not resolve to a persistent Map" );

		interpretSelect( "select value(b) from EntityOfLists e join e.listOfBasics b" );
	}

	@Override
	protected boolean strictJpaCompliance() {
		return true;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
	}

	public static class DTO {
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
