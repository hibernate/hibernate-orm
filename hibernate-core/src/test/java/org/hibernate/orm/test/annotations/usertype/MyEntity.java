package org.hibernate.orm.test.annotations.usertype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Type;

@Entity
class MyEntity {
    @Id
    @Type(MyType.class)
    MyId id;

    String content;

    MyEntity(MyId id, String content) {
        this.id = id;
        this.content = content;
    }

    MyEntity() {
    }
}
