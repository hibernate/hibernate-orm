package org.hibernate.orm.test.annotations.refcolnames.mixed;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
    String town;
}