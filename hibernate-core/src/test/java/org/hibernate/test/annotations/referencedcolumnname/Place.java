package org.hibernate.test.annotations.referencedcolumnname;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Janario Oliveira
 */
@Entity
public class Place {

	@Id
	@GeneratedValue
	int id;
	@Column(name = "NAME")
	String name;
	@Column(name = "OWNER")
	String owner;
}
