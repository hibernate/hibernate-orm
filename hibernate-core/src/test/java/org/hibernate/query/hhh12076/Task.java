/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Task<T> {

	private Long _id;
	private Integer _version;
	private Date _creationDate;
	private Date _modifiedDate;

	private Date _startDate;
	private Date _closeDate;
	private Date _dueDate;
	private Date _stateDueDate;
	private Date _statusDueDate;
	private Date _stateTransitionDate;
	private Date _statusTransitionDate;

	private Task<?> _parent;
	private TaskStatus _status;

	private Set<Task<?>> _children;
	private Set<Task<?>> _linkedTasks;

	public Task() {
		_children = new HashSet<Task<?>>();
		_linkedTasks = new HashSet<Task<?>>();
	}

	public abstract T getLinked();

	public abstract void setLinked(T linked);

	public void addChild(Task<?> task) {
		task.setParent( this );
		_children.add( task );
	}

	public Long getId() {
		return _id;
	}

	public void setId(Long id) {
		_id = id;
	}

	public Integer getVersion() {
		return _version;
	}

	public void setVersion(Integer version) {
		_version = version;
	}

	public Date getCreationDate() {
		return _creationDate;
	}

	public void setCreationDate(Date creationDate) {
		_creationDate = creationDate;
	}

	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		_modifiedDate = modifiedDate;
	}

	public TaskStatus getStatus() {
		return _status;
	}

	public void setStatus(TaskStatus status) {
		_status = status;
	}

	@SuppressWarnings("unchecked")
	public <X extends Task<?>> Set<X> getChildren(Class<X> ofType) {
		Set<X> children = null;

		children = new LinkedHashSet<X>();
		for ( Task<?> child : _children ) {
			if ( ofType.isInstance( child ) ) {
				children.add( (X) child );
			}
		}
		return children;
	}

	public Set<Task<?>> getChildren() {
		return _children;
	}

	public void setChildren(Set<Task<?>> links) {
		_children = links;
	}

	public Date getStartDate() {
		return _startDate;
	}

	public void setStartDate(Date openDate) {
		_startDate = openDate;
	}

	public Date getCloseDate() {
		return _closeDate;
	}

	public void setCloseDate(Date closeDate) {
		_closeDate = closeDate;
	}

	public Date getDueDate() {
		return _dueDate;
	}

	public void setDueDate(Date expiryDate) {
		_dueDate = expiryDate;
	}

	public Task<?> getParent() {
		return _parent;
	}

	public void setParent(Task<?> parentTask) {
		_parent = parentTask;
	}

	public Set<Task<?>> getLinkedTasks() {
		return _linkedTasks;
	}

	public void setLinkedTasks(Set<Task<?>> linkedTasks) {
		_linkedTasks = linkedTasks;
	}

	public Date getStateTransitionDate() {
		return _stateTransitionDate;
	}

	public void setStateTransitionDate(Date stateTransitionDate) {
		_stateTransitionDate = stateTransitionDate;
	}

	public Date getStatusTransitionDate() {
		return _statusTransitionDate;
	}

	public void setStatusTransitionDate(Date taskTransitionDate) {
		_statusTransitionDate = taskTransitionDate;
	}

	public Date getStateDueDate() {
		return _stateDueDate;
	}

	public void setStateDueDate(Date stateDueDate) {
		_stateDueDate = stateDueDate;
	}

	public Date getStatusDueDate() {
		return _statusDueDate;
	}

	public void setStatusDueDate(Date statusDueDate) {
		_statusDueDate = statusDueDate;
	}

}
