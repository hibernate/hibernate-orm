/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.hibernate.orm.test.query.resultmapping.EntityWithEmbedded.IMPLICIT;


/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "entity_with_embedded" )
@SqlResultSetMapping(
		name = IMPLICIT,
		entities = @EntityResult( entityClass = EntityWithEmbedded.class )
)
@SqlResultSetMapping(
		name = EntityWithEmbedded.EXPLICIT,
		entities = @EntityResult(
				entityClass = EntityWithEmbedded.class,
				fields = {
						@FieldResult( name = "id", column = "id_alias" ),
						@FieldResult( name = "compoundName.firstPart", column = "name_first_part_alias" ),
						@FieldResult( name = "compoundName.secondPart", column = "name_second_part_alias" )
				}
		)
)
@NamedNativeQuery(
		name = EntityWithEmbedded.EXPLICIT,
		query =
				"select id as id_alias,"
				+ " name_first_part as name_first_part_alias,"
				+ " name_second_part as name_second_part_alias"
				+ " from entity_with_embedded",
		resultSetMapping = EntityWithEmbedded.EXPLICIT
)
@NamedNativeQuery(
		name = IMPLICIT,
		query =
				"select id,"
				+ " name_first_part,"
				+ " name_second_part"
				+ " from entity_with_embedded",
		resultSetMapping = IMPLICIT
)
public class EntityWithEmbedded {

	public static final String IMPLICIT = "entity-with-embedded-implicit";
	public static final String EXPLICIT = "entity-with-embedded-explicit";

	private Integer id;
	private CompoundName compoundName;

	public EntityWithEmbedded() {
	}

	public EntityWithEmbedded(Integer id, CompoundName compoundName) {
		this.id = id;
		this.compoundName = compoundName;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	public CompoundName getCompoundName() {
		return compoundName;
	}

	public void setCompoundName(CompoundName compoundName) {
		this.compoundName = compoundName;
	}

	@Embeddable
	public static class CompoundName {
		private String firstPart;
		private String secondPart;

		public CompoundName() {
		}

		public CompoundName(String firstPart, String secondPart) {
			this.firstPart = firstPart;
			this.secondPart = secondPart;
		}

		@Column( name = "name_first_part" )
		public String getFirstPart() {
			return firstPart;
		}

		public void setFirstPart(String firstPart) {
			this.firstPart = firstPart;
		}

		@Column( name = "name_second_part" )
		public String getSecondPart() {
			return secondPart;
		}

		public void setSecondPart(String secondPart) {
			this.secondPart = secondPart;
		}

		@Override
		public String toString() {
			return firstPart + "." + secondPart;
		}
	}
}
