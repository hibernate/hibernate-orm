//$Id: PersistEvent.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event;



/** 
 * An event class for persist()
 *
 * @author Gavin King
 */
public class PersistEvent extends AbstractEvent {

	private Object object;
	private String entityName;

	public PersistEvent(String entityName, Object original, EventSource source) {
		this(original, source);
		this.entityName = entityName;
	}

	public PersistEvent(Object object, EventSource source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create create event with null entity"
			);
		}
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
