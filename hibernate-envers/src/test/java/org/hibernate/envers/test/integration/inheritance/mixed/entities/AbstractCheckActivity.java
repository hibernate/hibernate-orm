package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import org.hibernate.envers.Audited;

@Audited
public abstract class AbstractCheckActivity extends AbstractActivity {
    private Integer durationInMinutes;
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
