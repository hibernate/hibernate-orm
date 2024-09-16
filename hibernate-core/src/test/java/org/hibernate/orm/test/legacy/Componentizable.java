/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;


/**
 * contains components
 *
 * @author emmanuel
 */
public class Componentizable {
	/** surrogate id */
	private Integer _id;

	public String _nickName;

	/** component */
	private Component _component;

	/**
	 * @return
	 */
	public Integer getId() {
		return _id;
	}

	/**
	 * @param integer
	 */
	public void setId(Integer integer) {
		_id = integer;
	}

	/**
	 * @return
	 */
	public Component getComponent() {
		return _component;
	}

	/**
	 * @param component
	 */
	public void setComponent(Component component) {
		_component = component;
	}

	/**
	 * @return
	 */
	public String getNickName() {
		return _nickName;
	}

	/**
	 * @param string
	 */
	public void setNickName(String string) {
		_nickName = string;
	}

}
