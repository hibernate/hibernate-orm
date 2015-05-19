/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stateless.fetching;
import java.util.Date;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Task {
	private Long id;
	private String description;
	private User user;
	private Resource resource;
	private Date dueDate;
	private Date startDate;
	private Date completionDate;

	public Task() {
	}

	public Task(User user, String description, Resource resource, Date dueDate) {
		this( user, description, resource, dueDate, null, null );
	}

	public Task(User user, String description, Resource resource, Date dueDate, Date startDate, Date completionDate) {
		this.user = user;
		this.resource = resource;
		this.description = description;
		this.dueDate = dueDate;
		this.startDate = startDate;
		this.completionDate = completionDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}
}
