/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.LockModeType;

public interface JaxbNamedQuery extends Serializable {
	String getName();
	void setName(String value);

	String getDescription();
	void setDescription(String value);

	String getQuery();
	void setQuery(String value);

	String getComment();
	void setComment(String comment);

	Integer getTimeout();
	void setTimeout(Integer timeout);

	LockModeType getLockMode();
	void setLockMode(LockModeType value);

	List<JaxbQueryHintImpl> getHint();
}
