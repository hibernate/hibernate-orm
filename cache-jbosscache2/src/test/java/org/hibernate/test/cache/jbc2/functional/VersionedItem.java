package org.hibernate.test.cache.jbc2.functional;

/**
 * @author Steve Ebersole
 */
public class VersionedItem extends Item {
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
