//$Id: DeleteEvent.java 7450 2005-07-11 20:33:59Z steveebersole $
package org.hibernate.event;


/** Defines an event class for the deletion of an entity.
 *
 * @author Steve Ebersole
 */
public class DeleteEvent extends AbstractEvent {

	private Object object;
	private String entityName;
	private boolean cascadeDeleteEnabled;

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

	public DeleteEvent(String entityName, Object object, boolean isCascadeDeleteEnabled, EventSource source) {
		this(object, source);
		this.entityName = entityName;
		cascadeDeleteEnabled = isCascadeDeleteEnabled;
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

}
