package org.hibernate.test.lazyload;

import javax.persistence.*;

@Entity
@Table(name = "MANY")
@DiscriminatorColumn(name = "TYPE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ParentManyEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENTITY_ID")
    private OneEntity oneEntity;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public OneEntity getOneEntity() {
        return oneEntity;
    }

    public void setOneEntity(OneEntity oneEntity) {
        this.oneEntity = oneEntity;
    }

}
