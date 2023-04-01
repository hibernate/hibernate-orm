package org.hibernate.jpamodelgen.test.records;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String postalCode) {
}
