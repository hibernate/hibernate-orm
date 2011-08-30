//$Id: Department.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Sample of method generator
 *
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Department implements Serializable {
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DEPT")
	@javax.persistence.SequenceGenerator(
			name = "SEQ_DEPT",
			sequenceName = "my_sequence"
	)
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
