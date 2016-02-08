package org.hibernate.userguide.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-examples-domain-model-example[]
//tag::jpql-api-named-query-example[]
@NamedQueries(
    @NamedQuery(
        name = "get_person_by_name",
        query = "select p from Person p where name = :name"
    )
)
//end::jpql-api-named-query-example[]
@Entity
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String nickName;

    private String address;

    @Temporal(TemporalType.TIMESTAMP )
    private Date createdOn;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    @OrderColumn(name = "order_id")
    private List<Phone> phones = new ArrayList<>();

    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map<AddressType, String> addresses = new HashMap<>();

    @Version
    private int version;

    //Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]

    public Person() {}

    public Person(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public Map<AddressType, String> getAddresses() {
        return addresses;
    }

    public void addPhone(Phone phone) {
        phones.add( phone );
        phone.setPerson( this );
    }
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]
