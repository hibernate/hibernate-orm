/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.community.dialect.AltibaseDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

/**
 * @author Normunds Gavars
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14227")
@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
public class InsertOrderingSelfReferenceTest extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parameter.class,
				InputParameter.class,
				OutputParameter.class,
				Placeholder.class,
		};
	}

	@Test
	public void testReferenceItself() {
		sessionFactoryScope().inTransaction( session -> {
			Placeholder placeholder = new Placeholder();
			session.persist( placeholder );

			OutputParameter outputParameter1 = new OutputParameter();

			OutputParameter childOutputParameter = new OutputParameter();
			outputParameter1.children.add( childOutputParameter );
			childOutputParameter.parent = outputParameter1;

			session.persist( outputParameter1 );

			Placeholder placeholder2 = new Placeholder();
			session.persist( placeholder2 );

			InputParameter inputParameter = new InputParameter();
			session.persist( inputParameter );

			OutputParameter outputParameter2 = new OutputParameter();
			session.persist( outputParameter2 );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into Placeholder (name,id) values (?,?)", 2 ),
				new Batch( List.of(
						"insert into Parameter (name,parent_id,TYPE,id) values (?,?," + literal( "INPUT" ) + ",?)",
						"insert into \"Parameter\" (name,\"parent_id\",TYPE,id) values (?,?," + literal( "INPUT" ) + ",?)" ) ),
				new Batch( List.of(
						"insert into Parameter (name,parent_id,TYPE,id) values (?,?," + literal( "OUTPUT" ) + ",?)",
						"insert into \"Parameter\" (name,\"parent_id\",TYPE,id) values (?,?," + literal( "OUTPUT" ) + ",?)" ),
						3 )
		);
	}

	@MappedSuperclass
	static class AbstractEntity {

		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "Placeholder")
	static class Placeholder extends AbstractEntity {
		String name;
	}

	@Entity(name = "Parameter")
	@DiscriminatorColumn(name = "TYPE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	static abstract class Parameter extends AbstractEntity {
		String name;
	}

	@Entity(name = "InputParameter")
	@DiscriminatorValue("INPUT")
	static class InputParameter extends Parameter {

		@ManyToOne(fetch = FetchType.LAZY)
		InputParameter parent;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
		@SortNatural
		@SQLRestriction("TYPE = 'INPUT'")
		@Fetch(FetchMode.SUBSELECT)
		List<InputParameter> children = new ArrayList<>();
	}

	@Entity(name = "OutputParameter")
	@DiscriminatorValue("OUTPUT")
	static class OutputParameter extends Parameter {

		@ManyToOne(fetch = FetchType.LAZY)
		OutputParameter parent;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
		@SortNatural
		@SQLRestriction("TYPE = 'OUTPUT'")
		@Fetch(FetchMode.SUBSELECT)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		List<OutputParameter> children = new ArrayList<>();

	}
}
