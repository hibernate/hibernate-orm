//$Id: Detail.java 4602 2004-09-26 11:42:47Z oneovthafew $
package org.hibernate.test.formulajoin;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Detail implements Serializable {
	private Long id;
	private Master master;
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
	public Master getMaster() {
		return master;
	}
	public void setMaster(Master master) {
		this.master = master;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
}
