package org.hibernate.test.mapping.joinformula;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ChildEntity {

    @Id
    private Long id;

    @Column(name = "PARENT_ID")
    private Long parentId;

    @Column
    private String name;
}
