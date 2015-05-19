/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.abstractembeddedcomponents.cid;
import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface MyInterface extends Serializable {
	public String getKey1();
	public void setKey1(String key1);
	public String getKey2();
	public void setKey2(String key2);
	public String getName();
	public void setName(String name);
}
