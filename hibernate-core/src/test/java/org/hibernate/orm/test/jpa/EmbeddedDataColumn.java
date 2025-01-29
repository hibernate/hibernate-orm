package org.hibernate.orm.test.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EmbeddedDataColumn {

    @Column
    public String embeddedData = null;
}
