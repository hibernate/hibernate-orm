package org.hibernate.envers.test.integration.components.dynamic;

import org.hibernate.envers.Audited;

@Audited
public class ManyToManyEntity {

	private Long id;
	private String note;

	public ManyToManyEntity() {
	}

	public ManyToManyEntity(Long id, String note) {
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
		if ( !( o instanceof ManyToManyEntity ) ) {
			return false;
		}

		ManyToManyEntity that = (ManyToManyEntity) o;

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
		return "ManyToManyEntity{" +
				"id=" + id +
				", note='" + note + '\'' +
				'}';
	}
}
