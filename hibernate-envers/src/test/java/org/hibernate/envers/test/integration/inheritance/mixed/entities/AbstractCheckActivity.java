package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import org.hibernate.envers.Audited;

import javax.persistence.*;

@Audited
@Entity
@DiscriminatorValue(value = "CHECK")
@SecondaryTable(name = "ACTIVITY_CHECK",
		pkJoinColumns = {@PrimaryKeyJoinColumn(name = "ACTIVITY_ID"),
						@PrimaryKeyJoinColumn(name = "ACTIVITY_ID2")})
public abstract class AbstractCheckActivity extends AbstractActivity {
    @Column(table = "ACTIVITY_CHECK")
    private Integer durationInMinutes;
    @ManyToOne(targetEntity = AbstractActivity.class, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
	@JoinColumns({@JoinColumn(table = "ACTIVITY_CHECK", referencedColumnName = "id"),
			@JoinColumn(table = "ACTIVITY_CHECK", referencedColumnName = "id2")})
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
