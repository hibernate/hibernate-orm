/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.referencedcolumnname;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Janario Oliveira
 */
@Embeddable
public class Places {

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "LIVING_ROOM", referencedColumnName = "NAME")
	@JoinColumn(name = "LIVING_ROOM_OWNER", referencedColumnName = "OWNER")
	Place livingRoom;
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "KITCHEN", referencedColumnName = "NAME")
	Place kitchen;
}
