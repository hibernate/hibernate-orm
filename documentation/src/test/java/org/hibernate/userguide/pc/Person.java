package org.hibernate.userguide.pc;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Vlad Mihalcea
 */
//tag::pc-cascade-domain-model-example[]
@Entity
public class Person {

    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Phone> phones = new ArrayList<>();

    //Getters and setters are omitted for brevity
//end::pc-cascade-domain-model-example[]

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Phone> getPhones() {
        return phones;
    }

//tag::pc-cascade-domain-model-example[]

    public void addPhone(Phone phone) {
        this.phones.add( phone );
        phone.setOwner( this );
    }
}

//end::pc-cascade-domain-model-example[]
