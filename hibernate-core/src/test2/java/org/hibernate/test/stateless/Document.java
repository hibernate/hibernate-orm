/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Document.java 8057 2005-08-31 23:19:53Z oneovthafew $
package org.hibernate.test.stateless;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Document {
	
	private String text;
	private String name;
	private Date lastModified;

	Document() {}
	
	public Document(String text, String name) {
		this.text = text;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getLastModified() {
		return lastModified;
	}

	void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

}
