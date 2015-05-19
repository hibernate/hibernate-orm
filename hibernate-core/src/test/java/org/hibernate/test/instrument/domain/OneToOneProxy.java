/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.domain;


/**
 * @author Gail Badner
 */
public class OneToOneProxy {
	private Long entityId;
	private String name;
	private EntityWithOneToOnes entity;

	public OneToOneProxy() {}
	public OneToOneProxy(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the id.
	 */
	public Long getEntityId() {
		return entityId;
	}
	/**
	 * @param entityId The id to set.
	 */
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	public EntityWithOneToOnes getEntity() {
		return entity;
	}
	public void setEntity(EntityWithOneToOnes entity) {
		this.entity = entity;
	}
}
