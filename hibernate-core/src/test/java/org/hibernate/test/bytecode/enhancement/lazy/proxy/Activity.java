/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Activity")
@Table(name = "activity")
@SuppressWarnings("WeakerAccess")
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
	@LazyToOne(LazyToOneOption.NO_PROXY)
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
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup("webApplication")
	@JoinColumn(name="web_app_oid")
	public WebApplication getWebApplication() {
		return webApplication;
	}

	public void setWebApplication(WebApplication webApplication) {
		this.webApplication = webApplication;
	}
}
