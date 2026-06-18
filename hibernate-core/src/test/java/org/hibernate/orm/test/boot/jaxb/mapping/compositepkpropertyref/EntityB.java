/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.compositepkpropertyref;


public class EntityB {
	private Integer code;
	private String label;
	private Long refId;
	private EntityA entityA;

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Long getRefId() {
		return refId;
	}

	public void setRefId(Long refId) {
		this.refId = refId;
	}

	public EntityA getEntityA() {
		return entityA;
	}

	public void setEntityA(EntityA entityA) {
		this.entityA = entityA;
	}
}
