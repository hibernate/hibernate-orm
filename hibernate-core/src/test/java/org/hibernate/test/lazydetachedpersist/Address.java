package org.hibernate.test.lazydetachedpersist;


import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "eg_sbt_address")
public class Address {

    private Long id;
    private String content;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Basic
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
