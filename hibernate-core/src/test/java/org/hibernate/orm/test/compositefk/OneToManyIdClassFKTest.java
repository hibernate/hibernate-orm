/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		OneToManyIdClassFKTest.Party.class,
		OneToManyIdClassFKTest.Location.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16274" )
public class OneToManyIdClassFKTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Party party = new Party();
			party.setPartyCode( "party_1" );
			party.setSeqNo( "seq_1" );
			party.setRefNo( "ref_1" );
			final Location location = new Location();
			location.setRefNo( "ref_1" );
			location.setSeqNo( "seq_1" );
			location.getParties().add( party );
			session.persist( location );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Party" ).executeUpdate();
			session.createMutationQuery( "delete from Location" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Location location = session.createQuery(
					"from Location", Location.class
			).getSingleResult();
			assertThat( location.getRefNo() ).isEqualTo( "ref_1" );
			assertThat( location.getSeqNo() ).isEqualTo( "seq_1" );
			assertThat( location.getParties() ).hasSize( 1 );
			assertThat( location.getParties().iterator().next().getPartyCode() ).isEqualTo( "party_1" );
		} );
	}

	public static class PartyId implements Serializable {
		private String refNo;
		private String seqNo;
		private String partyCode;

		@Column( name = "REF_NO" )
		public String getRefNo() {
			return refNo;
		}

		public void setRefNo(String refNo) {
			this.refNo = refNo;
		}

		@Column( name = "SEQ_NO" )
		public String getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(String seqNo) {
			this.seqNo = seqNo;
		}

		@Column( name = "PARTY_CODE" )
		public String getPartyCode() {
			return partyCode;
		}

		public void setPartyCode(String partyCode) {
			this.partyCode = partyCode;
		}
	}


	@Entity( name = "Party" )
	@Table( name = "parties" )
	@IdClass( PartyId.class )
	public static class Party {
		private String refNo;
		private String seqNo;
		private String partyCode;

		@Id
		@Column( name = "REF_NO" )
		public String getRefNo() {
			return refNo;
		}


		public void setRefNo(String refNo) {
			this.refNo = refNo;
		}

		@Id
		@Column( name = "SEQ_NO" )
		public String getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(String seqNo) {
			this.seqNo = seqNo;
		}

		@Id
		@Column( name = "PARTY_CODE" )
		public String getPartyCode() {
			return partyCode;
		}

		public void setPartyCode(String partyCode) {
			this.partyCode = partyCode;
		}
	}

	public static class GuaranteeId implements Serializable {
		private String refNo;
		private String seqNo;

		@Id // note : this causes the issue (wrong foreign key column order)
		@Column( name = "REF_NO" )
		public String getRefNo() {
			return refNo;
		}

		public void setRefNo(String refNo) {
			this.refNo = refNo;
		}

		@Id // note : this causes the issue (wrong foreign key column order)
		@Column( name = "SEQ_NO" )
		public String getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(String seqNo) {
			this.seqNo = seqNo;
		}
	}


	@Entity( name = "Location" )
	@Table( name = "locations" )
	@IdClass( GuaranteeId.class )
	public static class Location {
		private String refNo;
		private String seqNo;
		private Set<Party> parties = new HashSet<>();

		@Id
		@Column( name = "REF_NO" )
		public String getRefNo() {
			return refNo;
		}

		public void setRefNo(String refNo) {
			this.refNo = refNo;
		}

		@Id
		@Column( name = "SEQ_NO" )
		public String getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(String seqNo) {
			this.seqNo = seqNo;
		}

		@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL )
		@JoinColumns( {
				@JoinColumn( name = "REF_NO", referencedColumnName = "REF_NO" ),
				@JoinColumn( name = "SEQ_NO", referencedColumnName = "SEQ_NO" )
		} )
		public Set<Party> getParties() {
			return parties;
		}

		public void setParties(Set<Party> parties) {
			this.parties = parties;
		}
	}
}
