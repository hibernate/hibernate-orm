package org.hibernate.ejb.test.callbacks;

import javax.persistence.*;

/**
 * Test entity with a lazy property which requires build time instrumentation.
 */
@Entity
public class EntityWithLazyProperty {

    public static final byte[] PRE_UPDATE_VALUE = new byte[]{0x2A, 0x2A, 0x2A, 0x2A};

    @Id
    @GeneratedValue
    private Long id;

    @Basic(fetch = FetchType.LAZY)
    private byte[] lazyData;

    private String someField;

    private boolean updateLazyFieldInPreUpdate;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public byte[] getLazyData() {
        return lazyData;
    }

    public void setLazyData(final byte[] lazyData) {
        this.lazyData = lazyData;
    }

    public String getSomeField() {
        return someField;
    }

    public void setSomeField(String someField) {
        this.someField = someField;
    }

    public boolean isUpdateLazyFieldInPreUpdate() {
        return updateLazyFieldInPreUpdate;
    }

    public void setUpdateLazyFieldInPreUpdate(boolean updateLazyFieldInPreUpdate) {
        this.updateLazyFieldInPreUpdate = updateLazyFieldInPreUpdate;
    }

    @PreUpdate
    public void onPreUpdate() {
        //Allow the update of the lazy field from within the pre update to check that this does not break things.
        if(isUpdateLazyFieldInPreUpdate()) {
            this.setLazyData(PRE_UPDATE_VALUE);
        }
    }
}