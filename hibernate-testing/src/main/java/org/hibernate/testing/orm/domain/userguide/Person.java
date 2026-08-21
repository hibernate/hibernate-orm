/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.EnumType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
//tag::sql-scalar-NamedNativeQuery-example[]
@NamedNativeQuery(
		name = "find_person_name",
		query =
				"SELECT name " +
						"FROM Person ",
		resultClass = String.class
)
//end::sql-scalar-NamedNativeQuery-example[]
//tag::sql-multiple-scalar-values-NamedNativeQuery-example[]
@NamedNativeQuery(
		name = "find_person_name_and_nickName",
		query =
				"SELECT " +
						"   name, " +
						"   nick_name " +
						"FROM Person "
)
//end::sql-multiple-scalar-values-NamedNativeQuery-example[]
// tag::sql-multiple-scalar-values-dto-NamedNativeQuery-example[]
@NamedNativeQuery(
		name = "find_person_name_and_nickName_dto",
		query =
				"select " +
						"   name, " +
						"   nick_name " +
						"from Person ",
		resultSetMapping = "name_and_nickName_dto"
)
//end::sql-multiple-scalar-values-dto-NamedNativeQuery-example[]
//tag::sql-entity-NamedNativeQuery-example[]
@NamedNativeQuery(
		name = "find_person_by_name",
		query =
				"select " +
						"   p.id AS id, " +
						"   p.name AS name, " +
						"   p.nick_name AS nick_name, " +
						"   p.address AS address, " +
						"   p.created_on AS created_on, " +
						"   p.version AS version " +
						"from Person p " +
						"where p.name LIKE :name",
		resultClass = Person.class
)
//end::sql-entity-NamedNativeQuery-example[]
//tag::sql-entity-associations-NamedNativeQuery-example[]
@NamedNativeQuery(
		name = "find_person_with_phones_by_name",
		query =
				"select " +
						"   pr.id AS pr_id, " +
						"   pr.name AS pr_name, " +
						"   pr.nick_name AS pr_nick_name, " +
						"   pr.address AS pr_address, " +
						"   pr.created_on AS pr_created_on, " +
						"   pr.version AS pr_version, " +
						"   ph.id AS ph_id, " +
						"   ph.person_id AS ph_person_id, " +
						"   ph.phone_number AS ph_number, " +
						"   ph.phone_type AS ph_type " +
						"from Person pr " +
						"join Phone ph ON pr.id = ph.person_id " +
						"where pr.name LIKE :name",
		resultSetMapping = "person_with_phones"
)
//end::sql-entity-associations-NamedNativeQuery-example[]
//tag::sql-entity-associations-NamedNativeQuery-example[]
@SqlResultSetMapping(
		name = "person_with_phones",
		entities = {
				@EntityResult(
						entityClass = Person.class,
						fields = {
								@FieldResult( name = "id", column = "pr_id" ),
								@FieldResult( name = "name", column = "pr_name" ),
								@FieldResult( name = "nickName", column = "pr_nick_name" ),
								@FieldResult( name = "address", column = "pr_address" ),
								@FieldResult( name = "createdOn", column = "pr_created_on" ),
								@FieldResult( name = "version", column = "pr_version" ),
						}
				),
				@EntityResult(
						entityClass = Phone.class,
						fields = {
								@FieldResult( name = "id", column = "ph_id" ),
								@FieldResult( name = "person", column = "ph_person_id" ),
								@FieldResult( name = "number", column = "ph_number" ),
								@FieldResult( name = "type", column = "ph_type" ),
						}
				)
		}
)
//end::sql-entity-associations-NamedNativeQuery-example[]
//tag::sql-multiple-scalar-values-dto-NamedNativeQuery-example[]
@SqlResultSetMapping(
		name = "name_and_nickName_dto",
		classes = @ConstructorResult(
				targetClass = PersonNames.class,
				columns = {
						@ColumnResult(name = "name"),
						@ColumnResult(name = "nick_name")
				}
		)
)
//end::sql-multiple-scalar-values-dto-NamedNativeQuery-example[]
//tag::hql-examples-domain-model-example[]
//tag::jpql-api-named-query-example[]
@NamedQuery(
		name = "get_person_by_name",
		query = "select p from Person p where name = :name"
)
//end::jpql-api-named-query-example[]
// tag::jpa-read-only-entities-native-example[]
@NamedQuery(
		name = "get_read_only_person_by_name",
		query = "select p from Person p where name = :name",
		hints = {
				@QueryHint(
						name = "org.hibernate.readOnly",
						value = "true"
				)
		}
)
//end::jpa-read-only-entities-native-example[]
@NamedQuery(
		name = "delete_person",
		query = "delete Person"
)
//tag::sql-sp-ref-cursor-oracle-named-query-example[]
@NamedStoredProcedureQuery(
		name = "sp_person_phones",
		procedureName = "sp_person_phones",
		parameters = {
				@StoredProcedureParameter(
						name = "personId",
						type = Long.class,
						mode = ParameterMode.IN
				),
				@StoredProcedureParameter(
						name = "personPhones",
						type = Class.class,
						mode = ParameterMode.REF_CURSOR
				)
		}
)
//end::sql-sp-ref-cursor-oracle-named-query-example[]
@Entity
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Column(name = "nick_name")
	private String nickName;

	private String address;

	@Column(name = "created_on")
	private LocalDateTime createdOn;

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

	public LocalDateTime getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(LocalDateTime createdOn) {
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
