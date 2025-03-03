/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of MoreCrazyIdFieldNameStuffEntity.
 *
 * @author Steve Ebersole
 */
public class MoreCrazyIdFieldNameStuffEntity {
	private Long moreCrazyIdFieldNameStuffEntity;
	private HeresAnotherCrazyIdFieldName heresAnotherCrazyIdFieldName; // silly ain't it ;)
	private String name;

	public MoreCrazyIdFieldNameStuffEntity() {
	}

	public MoreCrazyIdFieldNameStuffEntity(String name) {
		this.name = name;
	}

	public Long getMoreCrazyIdFieldNameStuffEntity() {
		return moreCrazyIdFieldNameStuffEntity;
	}

	public void setMoreCrazyIdFieldNameStuffEntity(Long moreCrazyIdFieldNameStuffEntity) {
		this.moreCrazyIdFieldNameStuffEntity = moreCrazyIdFieldNameStuffEntity;
	}

	public HeresAnotherCrazyIdFieldName getHeresAnotherCrazyIdFieldName() {
		return heresAnotherCrazyIdFieldName;
	}

	public void setHeresAnotherCrazyIdFieldName(HeresAnotherCrazyIdFieldName heresAnotherCrazyIdFieldName) {
		this.heresAnotherCrazyIdFieldName = heresAnotherCrazyIdFieldName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
