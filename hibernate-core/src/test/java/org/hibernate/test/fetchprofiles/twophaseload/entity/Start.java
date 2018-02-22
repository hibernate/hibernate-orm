/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.twophaseload.entity;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfile.FetchOverride;

import javax.persistence.*;

import static org.hibernate.test.fetchprofiles.twophaseload.entity.EntityLoadedInTwoPhaseLoadTest.FETCH_PROFILE_NAME;

@Entity
@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
		@FetchOverride(entity = Start.class, association = "via1", mode = FetchMode.JOIN),
		@FetchOverride(entity = Start.class, association = "via2", mode = FetchMode.JOIN)
})
public class Start {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private Via1 via1;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private Via2 via2;

	public Start() {
	}

	public Start(Via1 via1, Via2 via2) {
		this.via1 = via1;
		this.via2 = via2;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Via1 getVia1() {
		return via1;
	}

	public void setVia1(Via1 via1) {
		this.via1 = via1;
	}

	public Via2 getVia2() {
		return via2;
	}

	public void setVia2(Via2 via2) {
		this.via2 = via2;
	}

}
