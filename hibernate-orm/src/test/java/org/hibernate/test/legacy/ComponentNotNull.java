/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: ComponentNotNull.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * Component used to check not-null sub properties management
 * 
 * @author Emmanuel Bernard
 */
public class ComponentNotNull {
	/* 
	 * I've flatten several components in one class, this is kind of ugly but
 	 * I don't have to write tons of classes
 	 */
	private String prop1Nullable;
	private String prop2Nullable;
	private ComponentNotNull supercomp;
	private ComponentNotNull subcomp;
	private String prop1Subcomp;

	/**
	 * @return
	 */
	public String getProp1Nullable() {
		return prop1Nullable;
	}

	/**
	 * @return
	 */
	public String getProp1Subcomp() {
		return prop1Subcomp;
	}

	/**
	 * @return
	 */
	public String getProp2Nullable() {
		return prop2Nullable;
	}

	/**
	 * @return
	 */
	public ComponentNotNull getSubcomp() {
		return subcomp;
	}

	/**
	 * @return
	 */
	public ComponentNotNull getSupercomp() {
		return supercomp;
	}

	/**
	 * @param string
	 */
	public void setProp1Nullable(String string) {
		prop1Nullable = string;
	}

	/**
	 * @param string
	 */
	public void setProp1Subcomp(String string) {
		prop1Subcomp = string;
	}

	/**
	 * @param string
	 */
	public void setProp2Nullable(String string) {
		prop2Nullable = string;
	}

	/**
	 * @param null1
	 */
	public void setSubcomp(ComponentNotNull null1) {
		subcomp = null1;
	}

	/**
	 * @param null1
	 */
	public void setSupercomp(ComponentNotNull null1) {
		supercomp = null1;
	}

}
