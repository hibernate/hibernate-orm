/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Vote")
@Table(name = "vote")
public class Vote {

	@Id
	private Long id;

	@Column(name = "vote_choice")
	@Convert( converter = YesNoConverter.class )
	private boolean voteChoice;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isVoteChoice() {
		return voteChoice;
	}

	public void setVoteChoice(boolean voteChoice) {
		this.voteChoice = voteChoice;
	}
}
