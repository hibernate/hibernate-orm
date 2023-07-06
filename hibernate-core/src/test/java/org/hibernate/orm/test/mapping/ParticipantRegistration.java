package org.hibernate.orm.test.mapping;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class ParticipantRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    private String registrationIndex;

    private String registrationDisplay;

    public String getRegistrationIndex() {
        return registrationIndex;
    }

    public void setRegistrationIndex(String registrationIndex) {
        this.registrationIndex = registrationIndex;
    }

    public String getRegistrationDisplay() {
        return registrationDisplay;
    }

    public void setRegistrationDisplay(String registrationDisplay) {
        this.registrationDisplay = registrationDisplay;
    }
}
