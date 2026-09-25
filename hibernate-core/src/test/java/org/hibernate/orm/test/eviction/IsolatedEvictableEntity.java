package org.hibernate.orm.test.eviction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class IsolatedEvictableEntity {
	@Id
	private Integer id;
	private String name;

	public IsolatedEvictableEntity() {
	}

	public IsolatedEvictableEntity(Integer id) {
		this.id = id;
	}
}
