/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;

/**
 * @author Steve Ebersole
 */
public interface SpeechInterface {
	Integer getId();
	void setId(Integer id);

	Double getLength();
	void setLength(Double length);

	String getName();
	void setName(String name);
}
