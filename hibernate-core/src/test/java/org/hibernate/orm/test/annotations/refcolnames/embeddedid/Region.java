package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
class Region {
    @EmbeddedId
    PostalCode postalCode;
}
