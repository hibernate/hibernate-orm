/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQuery;

import java.time.LocalDateTime;

@NamedQuery(name = "TextById",
		query = "select text from Record where id = ?1")
@NamedQuery(name = "AllRecords",
		query = "from Record order by timestamp, id")
@NamedQuery(name = "AllRecordsAsTuples",
		query = "select id, text from Record order by timestamp, id")
@NamedEntityGraph(name = "CompleteRecord",
		includeAllAttributes = true)
@Entity
public class Record {
	@Id @GeneratedValue
	String id;
	String text;
	LocalDateTime timestamp;
	Record() {}
	public Record(String text, LocalDateTime timestamp) {
		this.text = text;
		this.timestamp = timestamp;
	}
}
