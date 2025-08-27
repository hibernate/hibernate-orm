/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;

/**
 * A mapping model object representing a named relational database array type.
 */
@Incubating
public class UserDefinedArrayType extends AbstractUserDefinedType {

	private Integer arraySqlTypeCode;
	private String elementTypeName;
	private Integer elementSqlTypeCode;
	private Integer elementDdlTypeCode;
	private Integer arrayLength;

	public UserDefinedArrayType(String contributor, Namespace namespace, Identifier physicalTypeName) {
		super( contributor, namespace, physicalTypeName );
	}

	public Integer getArraySqlTypeCode() {
		return arraySqlTypeCode;
	}

	public void setArraySqlTypeCode(Integer arraySqlTypeCode) {
		this.arraySqlTypeCode = arraySqlTypeCode;
	}

	public String getElementTypeName() {
		return elementTypeName;
	}

	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	public Integer getElementSqlTypeCode() {
		return elementSqlTypeCode;
	}

	public void setElementSqlTypeCode(Integer elementSqlTypeCode) {
		this.elementSqlTypeCode = elementSqlTypeCode;
	}

	public Integer getElementDdlTypeCode() {
		return elementDdlTypeCode;
	}

	public void setElementDdlTypeCode(Integer elementDdlTypeCode) {
		this.elementDdlTypeCode = elementDdlTypeCode;
	}

	public Integer getArrayLength() {
		return arrayLength;
	}

	public void setArrayLength(Integer arrayLength) {
		this.arrayLength = arrayLength;
	}
}
