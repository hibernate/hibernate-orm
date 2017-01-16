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
public class EntityA {

	private Integer id;
	private Integer version;
	private EntityB entityB;

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

	@OneToOne(mappedBy = "entityA")
	public EntityB getEntityB() {
		return entityB;
	}

	public void setEntityB(EntityB entityB) {
		this.entityB = entityB;
	}

}
