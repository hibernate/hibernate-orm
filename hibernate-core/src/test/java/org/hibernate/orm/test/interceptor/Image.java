/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "image")
public class Image {
	@Id
	@GeneratedValue
	private Long id;
	@Column(nullable = false)
	private String name;
	@Embedded
	private Details details;

	public Details getDetails() {
		return details;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setDetails(Details details) {
		this.details = details;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return "Image/" + ( details == null ? "no details" : details.toString() );
	}

	@Embeddable
	public static class Details {
		@Column(name = "permissions", nullable = false)
		private long perm1 = -1;
		@Column(name = "comment_txt")
		private String comment;

		protected long getPerm1() {
			return this.perm1;
		}

		protected void setPerm1(long value) {
			this.perm1 = value;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public String toString() {
			return "Details=" + perm1;
		}
	}

}
