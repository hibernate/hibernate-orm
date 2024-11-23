package org.hibernate.query.criteria.internal.hhh13058;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@Entity(name = "Task")
@Table(name = "Task")
public class Task {

	@Id
	@GeneratedValue
	Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	Patient patient;

	String description;

	public Task() {
	}

	public Task(Patient patient) {
		this.patient = patient;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Task task = (Task) o;
		return id.equals( task.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return String.format( "Task(id: %d; description: %s)", id, description == null ? "null" : description );
	}
}
