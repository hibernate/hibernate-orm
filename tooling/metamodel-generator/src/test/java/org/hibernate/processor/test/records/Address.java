package org.hibernate.processor.test.records;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String postalCode) {
}
