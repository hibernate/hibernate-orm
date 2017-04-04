/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import org.hibernate.envers.Audited;

@Audited
@Entity
@DiscriminatorValue(value = "CHECK")
@SecondaryTable(name = "ACTIVITY_CHECK",
				pkJoinColumns = {
						@PrimaryKeyJoinColumn(name = "ACTIVITY_ID"),
						@PrimaryKeyJoinColumn(name = "ACTIVITY_ID2")
				})
public abstract class AbstractCheckActivity extends AbstractActivity {
	@Column(table = "ACTIVITY_CHECK")
	private Integer durationInMinutes;
	@ManyToOne(targetEntity = AbstractActivity.class, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumns({
						 @JoinColumn(table = "ACTIVITY_CHECK", referencedColumnName = "id"),
						 @JoinColumn(table = "ACTIVITY_CHECK", referencedColumnName = "id2")
				 })
	private Activity relatedActivity;

	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(Integer durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	public Activity getRelatedActivity() {
		return relatedActivity;
	}

	public void setRelatedActivity(Activity relatedActivity) {
		this.relatedActivity = relatedActivity;
	}
}
