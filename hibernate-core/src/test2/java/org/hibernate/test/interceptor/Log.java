/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Log.java 7700 2005-07-30 05:02:47Z oneovthafew $
package org.hibernate.test.interceptor;
import java.util.Calendar;

public class Log {
	private Long id;
	private String entityName;
	private String entityId;
	private String action;
	private Calendar time;
	
	public Log(String action, String id, String name) {
		super();
		this.action = action;
		entityId = id;
		entityName = name;
		time = Calendar.getInstance();
	}
	public Log() {
		super();
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Calendar getTime() {
		return time;
	}
	public void setTime(Calendar time) {
		this.time = time;
	}
}
