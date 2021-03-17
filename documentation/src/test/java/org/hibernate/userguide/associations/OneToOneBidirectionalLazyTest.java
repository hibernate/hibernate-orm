/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneBidirectionalLazyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Phone.class,
				PhoneDetails.class,
		};
	}

	@Test
	public void testLifecycle() {
		
	}
	
	//tag::associations-one-to-one-bidirectional-lazy-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		@OneToOne(
			mappedBy = "phone",
			cascade = CascadeType.ALL,
			orphanRemoval = true,
			fetch = FetchType.LAZY
		)
		@LazyToOne( LazyToOneOption.NO_PROXY )
		private PhoneDetails details;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-bidirectional-lazy-example[]

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public PhoneDetails getDetails() {
			return details;
		}

	//tag::associations-one-to-one-bidirectional-lazy-example[]
		public void addDetails(PhoneDetails details) {
			details.setPhone( this );
			this.details = details;
		}

		public void removeDetails() {
			if ( details != null ) {
				details.setPhone( null );
				this.details = null;
			}
		}
	}

	@Entity(name = "PhoneDetails")
	public static class PhoneDetails {

		@Id
		@GeneratedValue
		private Long id;

		private String provider;

		private String technology;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "phone_id")
		private Phone phone;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-bidirectional-lazy-example[]

		public PhoneDetails() {
		}

		public PhoneDetails(String provider, String technology) {
			this.provider = provider;
			this.technology = technology;
		}
		//Getters and setters are omitted for brevity

		public String getProvider() {
			return provider;
		}

		public String getTechnology() {
			return technology;
		}

		public void setTechnology(String technology) {
			this.technology = technology;
		}

		public Phone getPhone() {
			return phone;
		}

		public void setPhone(Phone phone) {
			this.phone = phone;
		}
	//tag::associations-one-to-one-bidirectional-lazy-example[]
	}
	//end::associations-one-to-one-bidirectional-lazy-example[]
}
