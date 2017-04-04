/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.merge;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;

import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class CompositeMergeTestTask extends AbstractEnhancerTestTask {

	private long entityId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {ParentEntity.class, Address.class, Country.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		ParentEntity parent = new ParentEntity();
		parent.description = "desc";
		parent.address = new Address();
		parent.address.street = "Sesame street";
		parent.address.country = new Country();
		parent.address.country.name = "Suriname";
		parent.address.country.languages = Arrays.asList( "english", "spanish" );

		parent.lazyField = new byte[100];

		Session s = getFactory().openSession();
		s.beginTransaction();
		s.persist( parent );
		s.getTransaction().commit();
		s.close();

		EnhancerTestUtils.checkDirtyTracking( parent );
		entityId = parent.id;
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();
		ParentEntity parent = s.get( ParentEntity.class, entityId );
		s.getTransaction().commit();
		s.close();

		EnhancerTestUtils.checkDirtyTracking( parent );

		parent.address.country.name = "Paraguai";

		EnhancerTestUtils.checkDirtyTracking( parent, "address.country" );

		s = getFactory().openSession();
		s.beginTransaction();
		ParentEntity mergedParent = (ParentEntity) s.merge( parent );
		EnhancerTestUtils.checkDirtyTracking( parent, "address.country" );
		EnhancerTestUtils.checkDirtyTracking( mergedParent, "address.country" );
		s.getTransaction().commit();
		s.close();

		EnhancerTestUtils.checkDirtyTracking( parent, "address.country" );
		EnhancerTestUtils.checkDirtyTracking( mergedParent );

		mergedParent.address.country.name = "Honduras";

		EnhancerTestUtils.checkDirtyTracking( mergedParent, "address.country" );

		s = getFactory().openSession();
		s.beginTransaction();
		s.saveOrUpdate( mergedParent );
		EnhancerTestUtils.checkDirtyTracking( mergedParent, "address.country" );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		parent = s.get( ParentEntity.class, entityId );
		s.getTransaction().commit();
		s.close();

		Assert.assertEquals( "Honduras", parent.address.country.name );
	}

	protected void cleanup() {
	}

	@Entity
	@Table(name = "parent_entity")
	private static class ParentEntity {

		@Id
		@GeneratedValue
		private long id;

		private String description;

		@Embedded
		private Address address;

		@Basic(fetch = FetchType.LAZY)
		private byte[] lazyField;

	}

	@Embeddable
	@Table(name = "address")
	private static class Address {

		private String street;

		@Embedded
		private Country country;

	}

	@Embeddable
	@Table(name = "country")
	private static class Country {

		private String name;

		@ElementCollection
		@CollectionTable(name = "languages", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
		List<String> languages;

	}

}
