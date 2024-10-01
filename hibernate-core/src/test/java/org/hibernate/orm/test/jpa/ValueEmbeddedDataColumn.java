package org.hibernate.orm.test.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ValueEmbeddedDataColumn<T> extends EmbeddedDataColumn {
    @Column
    public T myvalue;
}
