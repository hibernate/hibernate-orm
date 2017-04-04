/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Chris Pheby
 */
public class MyDate implements Serializable {

	private static final long serialVersionUID = -416056386419355705L;

	private Date date;

	public MyDate() {
	}
	
	public MyDate(Date date) {
		this.date = date;
	}
	
	public Date getDate() {
		return date;
	}
}
