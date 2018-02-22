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
		@FetchOverride(entity = Mid.class, association = "finish", mode = FetchMode.JOIN)
})
public class Mid {

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private Finish finish;

	public Mid() {
	}

	public Mid(Finish finish) {
		this.finish = finish;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Finish getFinish() {
		return finish;
	}

	public void setFinish(Finish finish) {
		this.finish = finish;
	}

}
