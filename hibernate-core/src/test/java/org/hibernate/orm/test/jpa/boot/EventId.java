/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
* @author Jan Schatteman
*/
@Entity(name = "EventId")
public class EventId {

	@Id
	@GeneratedValue
	private Long id;

}
