/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.collection;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-8564")
public class EmbeddableWithOneToMany_HHH_8564_Test
		extends AbstractEmbeddableWithManyToManyTest {

	@Override
	protected void addAnnotatedClasses(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClasses( User.class );
	}

	@Embeddable
	public static class Address {

		@ElementCollection(fetch = FetchType.EAGER)
		@Enumerated(EnumType.STRING)
		private Set<AddressType> type;

		@NotNull
		@Size(min = 3, max = 200)
		private String street;

		@NotNull
		@Pattern(regexp = "[0-9]{5}")
		private String zipcode;

		@NotNull
		@Size(min = 3, max = 60)
		private String city;

		@NotNull
		@Size(min = 3, max = 60)
		private String state;

	}

	public enum AddressType {

		OFFICE, HOME, BILLING

	}

	@Entity
	@Table(name = "users")
	@SuppressWarnings("serial")
	public static class User implements Serializable {

		@Id
		@NotNull
		private String email;

		@NotNull
		private String password;

		@NotNull
		private String name;

		@NotNull
		private String surname;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<Address> addresses;

		@Version
		private long version;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( email == null ) ?
					0 :
					email.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			User other = (User) obj;
			if ( email == null ) {
				if ( other.email != null ) {
					return false;
				}
			}
			else if ( !email.equals( other.email ) ) {
				return false;
			}
			return true;
		}

	}
}
