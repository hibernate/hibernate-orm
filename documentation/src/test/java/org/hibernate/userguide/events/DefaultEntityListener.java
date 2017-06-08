package org.hibernate.userguide.events;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Vlad Mihalcea
 */
//tag::events-default-listener-mapping-example[]
public class DefaultEntityListener {

    public void onPersist(Object entity) {
        if ( entity instanceof BaseEntity ) {
            BaseEntity baseEntity = (BaseEntity) entity;
            baseEntity.setCreatedOn( now() );
        }
    }

    public void onUpdate(Object entity) {
        if ( entity instanceof BaseEntity ) {
            BaseEntity baseEntity = (BaseEntity) entity;
            baseEntity.setUpdatedOn( now() );
        }
    }

    private Timestamp now() {
        return Timestamp.from(
            LocalDateTime.now().toInstant( ZoneOffset.UTC )
        );
    }
}
//end::events-default-listener-mapping-example[]
