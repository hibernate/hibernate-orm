/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.mapjoin;

import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

public class MapJoinTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { MapEntity.class, MapEntityLocal.class };
	}
	@Test
	public void allEntities() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MapEntity> query = cb.createQuery(MapEntity.class);

		Root<MapEntity> entity = query.from(MapEntity.class);
		MapJoin<MapEntity, String, MapEntityLocal> cname = (MapJoin) entity.join( "localized" );
		
		query = query
			.select(entity)
			.where( 
				cb.equal( cname.key(), "en" ) 
			)
			.orderBy( cb.asc( cb.upper( cname.value().get( "shortName" ) ) ) );
		
		em.createQuery(query).getResultList();
		
		em.getTransaction().commit();
		em.close();
	}

	@Entity
	@Table( name = "MAP_ENTITY" )
	public static class MapEntity {

		@Id
		@Column(name="key_")
		private String key;

		@ElementCollection(fetch= FetchType.LAZY)
		@CollectionTable(name="MAP_ENTITY_NAME", joinColumns=@JoinColumn(name="key_"))
		@MapKeyColumn(name="lang_")
		private Map<String, MapEntityLocal> localized;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}

	@Embeddable
	public static class MapEntityLocal {

		@Column(name="short_name")
		private String shortName;

		public String getShortName() {
			return shortName;
		}

		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
	}
}
