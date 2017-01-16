/**
 * 
 */
package org.hibernate.test.annotations.onetoone.version;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Entity
public class EntityB {

	private Integer id;
	private Integer version;
	private EntityA entityA;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Version
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@OneToOne
	public EntityA getEntityA() {
		return entityA;
	}

	public void setEntityA(EntityA entityA) {
		this.entityA = entityA;
	}

}
