/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

public class XmlEntity {

	private long _id;
	private AnnotationEntity _association;

	public long getId() {
		return _id;
	}

	public void setId(long id) {
		_id = id;
	}

	public AnnotationEntity getAssociation() {
		return _association;
	}

	public void setAssociation(AnnotationEntity association) {
		_association = association;
	}
}
