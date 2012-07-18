//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Plant {
	@Id
	private String id;
	private String name;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@PrePersist
	private void defineId() {
		//some (stupid) id generation
		if ( name.length() > 5 ) {
			setId( name.substring( 0, 5 ) );
		}
		else {
			setId( name );
		}
	}
}
