/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Detail.java 4602 2004-09-26 11:42:47Z oneovthafew $
package org.hibernate.test.formulajoin;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Detail implements Serializable {
	private Long id;
	private Root root;
	private int version;
	private String details;
	private boolean currentVersion;
	
	public boolean isCurrentVersion() {
		return currentVersion;
	}
	public void setCurrentVersion(boolean currentVersion) {
		this.currentVersion = currentVersion;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Root getRoot() {
		return root;
	}
	public void setRoot(Root root) {
		this.root = root;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
}
