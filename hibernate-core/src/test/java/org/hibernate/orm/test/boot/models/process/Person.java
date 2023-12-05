/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import java.util.UUID;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.ParamDef;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.orm.test.boot.models.MyUuidConverter;
import org.hibernate.type.descriptor.java.StringJavaType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Person")
@Table(name = "persons")
@SequenceGenerator(name = "seq_gen", sequenceName = "id_seq")
@TableGenerator(name = "tbl_gen", table = "id_tbl")
@GenericGenerator(name = "increment_gen", type = IncrementGenerator.class)
@JavaTypeRegistration(javaType = String.class, descriptorClass = StringJavaType.class)
@ConverterRegistration(domainType = UUID.class, converter = MyUuidConverter.class)
@NamedQuery(name = "jpaHql", query = "from Person")
@NamedNativeQuery(name = "jpaNative", query = "select * from persons")
@NamedStoredProcedureQuery(name = "jpaCallable", procedureName = "jpa_callable")
@org.hibernate.annotations.NamedQuery(name = "ormHql", query = "from Person")
@org.hibernate.annotations.NamedNativeQuery(name = "ormNative", query = "select * from persons")
@FilterDef(name = "name_filter", defaultCondition = "name = :name", parameters = {@ParamDef(name = "name", type = String.class)})
public class Person {
	@Id
	private Integer id;
	private String name;
}
