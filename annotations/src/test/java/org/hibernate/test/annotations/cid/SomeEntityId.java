package org.hibernate.test.annotations.cid;

import java.io.Serializable;
import javax.persistence.Embeddable;

import org.hibernate.annotations.*;

/**
 * @author bartek
 */
@Embeddable
public class SomeEntityId implements Serializable {
	private Integer id;

	private Integer version;

	@org.hibernate.annotations.Parent
	private SomeEntity parent;

	/**
	 *
	 */
	public SomeEntityId() {
		super();
	}

	/**
	 * @param i
	 * @param j
	 */
	public SomeEntityId(int id, int version) {
		super();
		this.id = id;
		this.version = version;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the parent
	 */
	public SomeEntity getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(SomeEntity parent) {
		this.parent = parent;
	}
}
