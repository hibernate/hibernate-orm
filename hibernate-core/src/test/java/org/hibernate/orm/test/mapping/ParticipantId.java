package org.hibernate.orm.test.mapping;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import java.io.Serializable;

@Embeddable
public class ParticipantId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride( name = "registrationIndex", column = @Column(name = "REGISTRATION_INDEX")),
            @AttributeOverride( name = "registrationDisplay", column = @Column(name = "REGISTRATION_INDEX", insertable=false, updatable=false)),
    })
    private ParticipantRegistration registrationNumber;

    public ParticipantRegistration getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(ParticipantRegistration registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
