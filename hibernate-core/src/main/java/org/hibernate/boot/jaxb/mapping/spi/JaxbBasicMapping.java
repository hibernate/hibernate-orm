/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * A model part that is (or can be) basic-valued - {@linkplain JaxbIdImpl}, {@linkplain JaxbBasicImpl} and
 * {@linkplain JaxbElementCollectionImpl}
 *
 * @author Steve Ebersole
 */
public interface JaxbBasicMapping {
	JaxbUserTypeImpl getType();

	void setType(JaxbUserTypeImpl value);

	String getTarget();

	void setTarget(String value);

	String getJavaType();

	void setJavaType(String value);

	String getJdbcType();

	void setJdbcType(String value);

	Integer getJdbcTypeCode();

	void setJdbcTypeCode(Integer value);

	String getJdbcTypeName();

	void setJdbcTypeName(String value);
}
