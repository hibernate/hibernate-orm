package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
    String town;
}