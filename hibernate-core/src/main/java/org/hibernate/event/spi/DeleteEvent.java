/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * Defines an event class for the deletion of an entity.
 *
 * @author Steve Ebersole
 */
public class DeleteEvent extends AbstractEvent {
	private Object object;
	private String entityName;
	private boolean cascadeDeleteEnabled;
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
	// ordering is improved.
	private boolean orphanRemovalBeforeUpdates;

	/**
	 * Constructs a new DeleteEvent instance.
	 *
	 * @param object The entity to be deleted.
	 * @param source The session from which the delete event was generated.
	 */
	public DeleteEvent(Object object, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException(
					"attempt to create delete event with null entity"
				);
		}
		this.object = object;
	}

	public DeleteEvent(String entityName, Object object, EventSource source) {
		this(object, source);
		this.entityName = entityName;
	}

	public DeleteEvent(String entityName, Object object, boolean cascadeDeleteEnabled, EventSource source) {
		this(object, source);
		this.entityName = entityName;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public DeleteEvent(String entityName, Object object, boolean cascadeDeleteEnabled,
			boolean orphanRemovalBeforeUpdates, EventSource source) {
		this(object, source);
		this.entityName = entityName;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.orphanRemovalBeforeUpdates = orphanRemovalBeforeUpdates;
	}

	/**
     * Returns the encapsulated entity to be deleed.
     *
     * @return The entity to be deleted.
     */
	public Object getObject() {
		return object;
	}

	public String getEntityName() {
		return entityName;
	}
	
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}
	
	public boolean isOrphanRemovalBeforeUpdates() {
		return orphanRemovalBeforeUpdates;
	}

}
