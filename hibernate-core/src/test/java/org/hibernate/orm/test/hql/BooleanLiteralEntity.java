/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * todo: describe BooleanLiteralEntity
 *
 * @author Steve Ebersole
 */
public class BooleanLiteralEntity {
	private Long id;
	private boolean yesNoBoolean;
	private boolean trueFalseBoolean;
	private boolean zeroOneBoolean;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isYesNoBoolean() {
		return yesNoBoolean;
	}

	public void setYesNoBoolean(boolean yesNoBoolean) {
		this.yesNoBoolean = yesNoBoolean;
	}

	public boolean isTrueFalseBoolean() {
		return trueFalseBoolean;
	}

	public void setTrueFalseBoolean(boolean trueFalseBoolean) {
		this.trueFalseBoolean = trueFalseBoolean;
	}

	public boolean isZeroOneBoolean() {
		return zeroOneBoolean;
	}

	public void setZeroOneBoolean(boolean zeroOneBoolean) {
		this.zeroOneBoolean = zeroOneBoolean;
	}
}
