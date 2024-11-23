package org.hibernate.query.criteria.internal.hhh14197;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Archie Cobbs
 */

@MappedSuperclass
public abstract class AbstractPersistent {

	private long id;

	protected AbstractPersistent() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	public long getId() {
		return this.id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
