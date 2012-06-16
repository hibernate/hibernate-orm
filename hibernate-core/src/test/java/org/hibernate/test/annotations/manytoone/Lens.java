//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Formula;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Lens {
	@Id
	@GeneratedValue
	private Long id;
	private float focal;
	@Formula("(1/focal)")
	private float length;
	@ManyToOne()
	@JoinColumn(name="`frame_fk`", referencedColumnName = "name")
	private Frame frame;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public float getFocal() {
		return focal;
	}

	public void setFocal(float focal) {
		this.focal = focal;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public Frame getFrame() {
		return frame;
	}

	public void setFrame(Frame frame) {
		this.frame = frame;
	}
}
