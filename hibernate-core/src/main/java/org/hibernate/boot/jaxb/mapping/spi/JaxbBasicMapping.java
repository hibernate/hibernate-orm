/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
