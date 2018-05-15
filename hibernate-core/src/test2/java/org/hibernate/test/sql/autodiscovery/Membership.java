/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.autodiscovery;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "t_membership")
public class Membership {
	private Long id;
	private User member;
	private Group group;
	private Date joinDate;
	private Float dues;

	public Membership() {
	}

	public Membership(User member, Group group) {
		this.member = member;
		this.group = group;
		this.joinDate = new Date();
	}

	public Membership(User member, Group group, Date joinDate, Float dues) {
		this.member = member;
		this.group = group;
		this.joinDate = joinDate;
		this.dues = dues;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	public User getMember() {
		return member;
	}

	public void setMember(User member) {
		this.member = member;
	}

	@ManyToOne
	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Date getJoinDate() {
		return joinDate;
	}

	public void setJoinDate(Date joinDate) {
		this.joinDate = joinDate;
	}

	public Float getDues() {
		return dues;
	}

	public void setDues(Float dues) {
		this.dues = dues;
	}
}
