package org.hibernate.orm.test.tenantpk;

import java.util.UUID;

public class TenantizedId {
    Long id;
    UUID tenantId;

    public TenantizedId(Long id, UUID tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

    TenantizedId() {}
}
