/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.NamedNativeQuery;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import static org.hibernate.jpa.HibernateHints.HINT_CALLABLE_FUNCTION;

/**
 * @author Vlad Mihalcea
 */
@NamedStoredProcedureQuery(
		name = "personAndPhonesFunction",
		procedureName = "fn_person_and_phones",
		resultSetMappings = "personWithPhonesMapping",
		parameters = @StoredProcedureParameter(type = Long.class),
		hints = @QueryHint(name = HINT_CALLABLE_FUNCTION, value = "true")
)
@NamedNativeQuery(
		name = "fn_person_and_phones_hana",
		query = "select \"pr.id\", \"pr.name\", \"pr.nickName\", \"pr.address\", \"pr.createdOn\", \"pr.version\", \"ph.id\", \"ph.person_id\", \"ph.phone_number\", \"ph.valid\" from fn_person_and_phones( ? )",
		resultSetMapping = "person_with_phones_hana"
)
@SqlResultSetMappings({
	@SqlResultSetMapping(
		name = "personWithPhonesMapping",
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

	public Person(Long id, String name) {
		this.id = id;
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
