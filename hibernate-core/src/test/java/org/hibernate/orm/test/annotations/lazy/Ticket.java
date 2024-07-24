/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.lazy;

import java.util.List;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="TICKETS")
public class Ticket {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name="title")
	private String title;

	@NaturalId
	private String guid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requester_guid", referencedColumnName = "guid")
	private Person requester;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_guid", referencedColumnName = "guid")
	private Person assignee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private Company company;

	@OneToOne(fetch = FetchType.LAZY)
	private Reference reference;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bug_guid", referencedColumnName = "guid")
	private Bug bug;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id")
	private List<Update> updates;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_guid", referencedColumnName = "guid")
	private List<ReplicatedUpdate> replicatedUpdates;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Person getRequester() {
		return requester;
	}

	public void setRequester(Person requester) {
		this.requester = requester;
	}

	public Person getAssignee() {
		return assignee;
	}

	public void setAssignee(Person assignee) {
		this.assignee = assignee;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Bug getBug() {
		return bug;
	}

	public void setBug(Bug bug) {
		this.bug = bug;
	}

	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}

	public List<Update> getUpdates() {
		return updates;
	}

	public void setUpdates(List<Update> updates) {
		this.updates = updates;
	}

	public List<ReplicatedUpdate> getReplicatedUpdates() {
		return replicatedUpdates;
	}

	public void setReplicatedUpdates(List<ReplicatedUpdate> replicatedUpdates) {
		this.replicatedUpdates = replicatedUpdates;
	}
}
