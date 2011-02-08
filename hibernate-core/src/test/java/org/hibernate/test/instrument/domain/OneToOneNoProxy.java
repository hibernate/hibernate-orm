package org.hibernate.test.instrument.domain;


/**
 * @author Gail Badner
 */
public class OneToOneNoProxy {
	private Long entityId;
	private String name;
	private EntityWithOneToOnes entity;

	public OneToOneNoProxy() {}
	public OneToOneNoProxy(String name) {
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
