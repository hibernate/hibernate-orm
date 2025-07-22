/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "reports")
public class Report {
	@Id
	private Integer id;
	@Version
	private Integer revision = -1;

	private String title;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "reporter_fk")
	private Person reporter;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "report_labels", joinColumns = @JoinColumn(name = "report_fk"))
	@Column(name = "txt")
	private Set<String> labels;

	public Report() {
	}

	public Report(Integer id, Person reporter) {
		this.id = id;
		this.reporter = reporter;
	}

	public Report(Integer id, Person reporter, String... labels) {
		this.id = id;
		this.reporter = reporter;
		this.labels = Helper.toSet( labels );
	}
}
