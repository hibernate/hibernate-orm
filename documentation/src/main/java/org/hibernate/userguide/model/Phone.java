/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TemporalType;

import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * @author Vlad Mihalcea
 */
//tag::jpql-api-hibernate-named-query-example[]
@NamedQueries({
    @NamedQuery(
        name = "get_phone_by_number",
        query = "select p " +
                "from Phone p " +
                "where p.number = :number",
        timeout = 1,
        readOnly = true
    )
})
//end::jpql-api-hibernate-named-query-example[]
//tag::sql-multiple-scalar-values-dto-NamedNativeQuery-hibernate-example[]
@NamedNativeQueries({
    @NamedNativeQuery(
        name = "get_person_phone_count",
        query = "SELECT pr.name AS name, count(*) AS phoneCount " +
                "FROM Phone p " +
                "JOIN Person pr ON pr.id = p.person_id " +
                "GROUP BY pr.name",
        resultSetMapping = "person_phone_count",
        timeout = 1,
        readOnly = true
    ),
})
@SqlResultSetMapping(
    name = "person_phone_count",
    classes = @ConstructorResult(
        targetClass = PersonPhoneCount.class,
        columns = {
            @ColumnResult(name = "name"),
            @ColumnResult(name = "phoneCount")
        }
    )
)
//end::sql-multiple-scalar-values-dto-NamedNativeQuery-hibernate-example[]
//tag::hql-examples-domain-model-example[]
@Entity
public class Phone {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;

    @Column(name = "phone_number")
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type")
    private PhoneType type;

    @OneToMany(mappedBy = "phone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Call> calls = new ArrayList<>(  );

    //tag::hql-collection-qualification-example[]
    @OneToMany(mappedBy = "phone")
    @MapKey(name = "timestamp")
    @MapKeyTemporal(TemporalType.TIMESTAMP )
    private Map<Date, Call> callHistory = new HashMap<>();
    //end::hql-collection-qualification-example[]

    @ElementCollection
    private List<Date> repairTimestamps = new ArrayList<>(  );

    //Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]
    public Phone() {}

    public Phone(String number) {
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public PhoneType getType() {
        return type;
    }

    public void setType(PhoneType type) {
        this.type = type;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public Map<Date, Call> getCallHistory() {
        return callHistory;
    }

    public List<Date> getRepairTimestamps() {
        return repairTimestamps;
    }

    public void addCall(Call call) {
        calls.add( call );
        callHistory.put( call.getTimestamp(), call );
        call.setPhone( this );
    }
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]
