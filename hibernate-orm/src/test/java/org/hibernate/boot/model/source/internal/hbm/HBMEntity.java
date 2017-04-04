package org.hibernate.boot.model.source.internal.hbm;

public class HBMEntity {

    private long _id;
    private AnnotationEntity _association;

    public long getId() {
        return _id;
    }

    public void setId(long id) {
        _id = id;
    }

    public AnnotationEntity getAssociation() {
        return _association;
    }

    public void setAssociation(AnnotationEntity association) {
        _association = association;
    }
}
