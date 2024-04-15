package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base;

import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Embedded
    protected EmbeddableType embeddedField;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public EmbeddableType getEmbeddedField() {
        return embeddedField;
    }

    public void setEmbeddedField(final EmbeddableType embeddedField) {
        this.embeddedField = embeddedField;
    }
}
