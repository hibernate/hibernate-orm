/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
