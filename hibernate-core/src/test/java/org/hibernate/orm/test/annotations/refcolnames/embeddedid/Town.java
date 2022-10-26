package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
class Town {
    @EmbeddedId
    TownCode townCode;

    @ManyToOne
    @JoinColumn(name = "zip_code", referencedColumnName = "zip_code", insertable = false, updatable = false)
    @JoinColumn(name = "country_code", referencedColumnName = "country_code", insertable = false, updatable = false)
    Region region;
}