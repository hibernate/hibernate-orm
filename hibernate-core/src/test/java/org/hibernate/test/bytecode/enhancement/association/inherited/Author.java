package org.hibernate.test.bytecode.enhancement.association.inherited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Author {

    @Id
    @GeneratedValue
    Long id;

    @OneToMany( fetch = FetchType.LAZY, mappedBy = "author" )
    List<ChildItem> items;

    // keep this field after 'items'
    String name;

}
