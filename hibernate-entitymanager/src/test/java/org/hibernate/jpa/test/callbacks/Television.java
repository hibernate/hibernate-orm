//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
@Entity
@EntityListeners({IncreaseListener.class})
public class Television extends VideoSystem {
	private Integer id;
	private RemoteControl control;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToOne(cascade = CascadeType.ALL)
	public RemoteControl getControl() {
		return control;
	}

	public void setControl(RemoteControl control) {
		this.control = control;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@PreUpdate
	public void isLast() {
		if ( isLast ) throw new IllegalStateException();
		isFirst = false;
		isLast = true;
		communication++;
	}

	@PrePersist
	public void prepareEntity() {
		//override a super method annotated with the same
		// event for it not to be called
		counter++;
	}
}
