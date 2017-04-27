/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.uniquekey;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

public class NaturalIdCachingTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                PropertyHolder.class,
                Property.class
        };
    }

    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
    }


    @Test
    public void test() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Property property = new Property( 1, 1, 1 );
			session.persist( property );
			session.persist( new PropertyHolder( 1, property ) );
			session.persist( new PropertyHolder( 2, property ) );
		} );

        assertThat(sessionFactory().getStatistics().getEntityInsertCount(), is(3L));
		sessionFactory().getStatistics().clear();

		doInHibernate( this::sessionFactory, session -> {
			session.byId( PropertyHolder.class ).load( 1 );
			session.byId( PropertyHolder.class ).load( 2 );
		} );

		assertThat( sessionFactory().getStatistics().getEntityLoadCount(), is(3L) );
		assertThat( sessionFactory().getStatistics().getPrepareStatementCount(), is(3L) );
    }

    @Entity(name = "PropertyHolder")
	public static class PropertyHolder implements Serializable {

		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
			@JoinColumn(name="PROP_CODE", referencedColumnName = "CODE"),
			@JoinColumn(name="PROP_ITEM", referencedColumnName = "ITEM")
		})
		private Property property;

		private String severalOtherFields = "Several other fields ...";

		protected PropertyHolder() {}

		public PropertyHolder(Integer id, Property property) {
			this.id = id;
			this.property = property;
		}

	}

	@Entity(name = "PropertyEntity")
	public static class Property implements Serializable {

		@Id
		private Integer id;

		@NaturalId
		private Integer code;

		@NaturalId
		private Integer item;

		private String description = "A description ...";

		protected Property(){}

		public Property(Integer id, Integer code, Integer item) {
			this.id = id;
			this.code = code;
			this.item = item;
		}
	}
}