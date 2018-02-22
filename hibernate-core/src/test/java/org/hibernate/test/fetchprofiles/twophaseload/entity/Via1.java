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
		@FetchOverride(entity = Via1.class, association = "mid", mode = FetchMode.JOIN)
})
public class Via1 {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private Mid mid;

	public Via1() {
	}

	public Via1(Mid mid) {
		this.mid = mid;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Mid getMid() {
		return mid;
	}

	public void setMid(Mid mid) {
		this.mid = mid;
	}

}
