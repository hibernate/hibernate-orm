/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import org.hibernate.envers.Audited;

@Audited
public class ManyToOneEntity {

	private Long id;
	private String note;

	public ManyToOneEntity() {
	}

	public ManyToOneEntity(Long id, String note) {
		this.id = id;
		this.note = note;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof ManyToOneEntity ) ) {
			return false;
		}

		ManyToOneEntity that = (ManyToOneEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( note != null ? !note.equals( that.note ) : that.note != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( note != null ? note.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "ManyToOneEntity{" +
				"id=" + id +
				", note='" + note + '\'' +
				'}';
	}
}
