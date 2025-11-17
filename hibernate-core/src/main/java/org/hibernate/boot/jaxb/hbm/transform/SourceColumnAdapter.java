/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
public interface SourceColumnAdapter {
	String getName();
	Boolean isNotNull();
	Boolean isUnique();
	Integer getLength();
	Integer getPrecision();
	Integer getScale();
	String getSqlType();

	String getComment();
	String getCheck();
	String getDefault();

	String getIndex();
	String getUniqueKey();

	String getRead();
	String getWrite();
}
