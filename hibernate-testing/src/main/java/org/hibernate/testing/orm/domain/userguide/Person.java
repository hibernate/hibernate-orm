/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
						"   p.id AS \"id\", " +
						"   p.name AS \"name\", " +
						"   p.nick_name AS \"nick_name\", " +
						"   p.address AS \"address\", " +
						"   p.created_on AS \"created_on\", " +
						"   p.version AS \"version\" " +
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
						"   pr.id AS \"pr.id\", " +
						"   pr.name AS \"pr.name\", " +
						"   pr.nick_name AS \"pr.nick_name\", " +
						"   pr.address AS \"pr.address\", " +
						"   pr.created_on AS \"pr.created_on\", " +
						"   pr.version AS \"pr.version\", " +
						"   ph.id AS \"ph.id\", " +
						"   ph.person_id AS \"ph.person_id\", " +
						"   ph.phone_number AS \"ph.number\", " +
						"   ph.phone_type AS \"ph.type\" " +
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
								@FieldResult( name = "id", column = "pr.id" ),
								@FieldResult( name = "name", column = "pr.name" ),
								@FieldResult( name = "nickName", column = "pr.nick_name" ),
								@FieldResult( name = "address", column = "pr.address" ),
								@FieldResult( name = "createdOn", column = "pr.created_on" ),
								@FieldResult( name = "version", column = "pr.version" ),
						}
				),
				@EntityResult(
						entityClass = Phone.class,
						fields = {
								@FieldResult( name = "id", column = "ph.id" ),
								@FieldResult( name = "person", column = "ph.person_id" ),
								@FieldResult( name = "number", column = "ph.number" ),
								@FieldResult( name = "type", column = "ph.type" ),
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
