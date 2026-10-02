package org.hibernate.processor.test.constructor;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class MapperSuperClassExtendingNonEntityWithInstanceGetEntityManager
		extends NonEntityWithInstanceGetEntityManager {

	@Id
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
