/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.annotations.LazyGroup;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Activity")
@Table(name = "activity")
public class Activity extends BaseEntity {
	private String description;
	private Instruction instruction;

	protected WebApplication webApplication = null;

	/**
	 * Used by Hibernate
	 */
	@SuppressWarnings("unused")
	public Activity() {
		super();
	}

	public Activity(Integer id, String description, Instruction instruction) {
		super( id );
		this.description = description;
		this.instruction = instruction;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
	@LazyGroup("Instruction")
	@JoinColumn(name = "Instruction_Id")
	public Instruction getInstruction() {
		return instruction;
	}

	@SuppressWarnings("unused")
	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
	}

	@SuppressWarnings("unused")
	@ManyToOne(fetch=FetchType.LAZY)
	@LazyGroup("webApplication")
	@JoinColumn(name="web_app_oid")
	public WebApplication getWebApplication() {
		return webApplication;
	}

	public void setWebApplication(WebApplication webApplication) {
		this.webApplication = webApplication;
	}
}
