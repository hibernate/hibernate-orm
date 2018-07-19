/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.annotations.NamedNativeQuery;

/**
 * @author Vlad Mihalcea
 */
@NamedNativeQuery(
    name = "fn_person_and_phones",
    query = "{ ? = call fn_person_and_phones( ? ) }",
    callable = true,
    resultSetMapping = "person_with_phones"
)
@NamedNativeQuery(
	    name = "fn_person_and_phones_hana",
	    query = "select \"pr.id\", \"pr.name\", \"pr.nickName\", \"pr.address\", \"pr.createdOn\", \"pr.version\", \"ph.id\", \"ph.person_id\", \"ph.phone_number\", \"ph.valid\" from fn_person_and_phones( ? )",
	    callable = false,
	    resultSetMapping = "person_with_phones_hana"
	)
@SqlResultSetMappings({
     @SqlResultSetMapping(
         name = "person_with_phones",
         entities = {
             @EntityResult(
                 entityClass = Person.class,
                 fields = {
                     @FieldResult( name = "id", column = "pr.id" ),
                     @FieldResult( name = "name", column = "pr.name" ),
                     @FieldResult( name = "nickName", column = "pr.nickName" ),
                     @FieldResult( name = "address", column = "pr.address" ),
                     @FieldResult( name = "createdOn", column = "pr.createdOn" ),
                     @FieldResult( name = "version", column = "pr.version" ),
                 }
             ),
             @EntityResult(
                 entityClass = Phone.class,
                 fields = {
                     @FieldResult( name = "id", column = "ph.id" ),
                     @FieldResult( name = "person", column = "ph.person_id" ),
                     @FieldResult( name = "number", column = "ph.phone_number" ),
                     @FieldResult( name = "valid", column = "ph.valid" )
                 }
             )
         }
     ),
     @SqlResultSetMapping(
         name = "person_with_phones_hana",
         entities = {
             @EntityResult(
                 entityClass = Person.class,
                 fields = {
                     @FieldResult( name = "id", column = "pr.id" ),
                     @FieldResult( name = "name", column = "pr.name" ),
                     @FieldResult( name = "nickName", column = "pr.nickName" ),
                     @FieldResult( name = "address", column = "pr.address" ),
                     @FieldResult( name = "createdOn", column = "pr.createdOn" ),
                     @FieldResult( name = "version", column = "pr.version" ),
                 }
             ),
             @EntityResult(
                 entityClass = Phone.class,
                 fields = {
                     @FieldResult( name = "id", column = "ph.id" ),
                     @FieldResult( name = "person", column = "ph.person_id" ),
                     @FieldResult( name = "number", column = "ph.phone_number" ),
                     @FieldResult( name = "valid", column = "ph.valid" )
                 }
             )
         }
     ),
})
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

    public void addPhone(Phone phone) {
        phones.add( phone );
        phone.setPerson( this );
    }
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]
