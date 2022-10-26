package org.hibernate.orm.test.annotations.refcolnames.basics;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
    String town;
}