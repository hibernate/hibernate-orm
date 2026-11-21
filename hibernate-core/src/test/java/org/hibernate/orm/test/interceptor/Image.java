package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;


@Entity
public class Image {

	private Long id;
	private String name;
	private Details details;

	@Embedded
	public Details getDetails() {
		return details;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	@Column(nullable = false)
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
		private long perm1 = -1; // all bits turned on.
		private String comment;

		@Column(name = "permissions", nullable = false)
		protected long getPerm1() {
			return this.perm1;
		}

		protected void setPerm1(long value) {
			this.perm1 = value;
		}

		@Column(name = "comment_txt")
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
