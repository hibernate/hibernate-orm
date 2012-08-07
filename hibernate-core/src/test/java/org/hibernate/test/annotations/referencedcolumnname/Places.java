package org.hibernate.test.annotations.referencedcolumnname;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 * @author Janario Oliveira
 */
@Embeddable
public class Places {

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumns({ @JoinColumn(name = "LIVING_ROOM", referencedColumnName = "NAME"),
			@JoinColumn(name = "LIVING_ROOM_OWNER", referencedColumnName = "OWNER") })
	Place livingRoom;
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "KITCHEN", referencedColumnName = "NAME")
	Place kitchen;
}
