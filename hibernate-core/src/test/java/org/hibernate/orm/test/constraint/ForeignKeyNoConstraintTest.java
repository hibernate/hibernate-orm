/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class ForeignKeyNoConstraintTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] {
				Car.class,
				VehicleNumber.class,
				Post.class,
				PostDetails.class
		};
	}

	@Test
	@JiraKey(value = "HHH-12975")
	public void testPrimaryKeyJoinColumnForeignKeyNoConstraint() {
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( "Car".equals( table.getName() ) ) {
					assertEquals( 0, table.getForeignKeyCollection().size() );
				}
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-12975")
	public void testMapsIdJoinColumnForeignKeyNoConstraint() {
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( "Post".equals( table.getName() ) ) {
					assertEquals( 0, table.getForeignKeyCollection().size() );
				}
			}
		}
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		private Integer id;

		@PrimaryKeyJoinColumn
		@OneToOne(optional = false)
		@JoinColumn(name = "V_ID", foreignKey = @ForeignKey( ConstraintMode.NO_CONSTRAINT ) )
		private VehicleNumber vehicleNumber;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public VehicleNumber getVehicleNumber() {
			return vehicleNumber;
		}

		public void setVehicleNumber(VehicleNumber vehicleNumber) {
			this.vehicleNumber = vehicleNumber;
		}
	}

	@Entity(name = "VehicleNumber")
	public static class VehicleNumber {
		@Id
		private Integer id;
		private String value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity(name = "Post")
	public static class Post {
		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY)
		@MapsId
		@JoinColumn(name = "PD_ID", foreignKey = @ForeignKey(name = "FK_PD", value = ConstraintMode.NO_CONSTRAINT))
		private PostDetails postDetails;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public PostDetails getPostDetails() {
			return postDetails;
		}

		public void setPostDetails(PostDetails postDetails) {
			this.postDetails = postDetails;
		}
	}

	@Entity(name = "PostDetails")
	public static class PostDetails {
		@Id
		private Integer id;
		private String userName;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}
	}
}
