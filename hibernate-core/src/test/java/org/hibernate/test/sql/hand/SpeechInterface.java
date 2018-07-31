/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $
package org.hibernate.test.sql.hand;


/**
 * @author Günther Demetz
 */
public interface SpeechInterface {
	
	public Integer getId();

	public void setId(Integer id);

	public Double getLength();
	

	public void setLength(Double length);

	public String getName();

	public void setName(String name);
}
