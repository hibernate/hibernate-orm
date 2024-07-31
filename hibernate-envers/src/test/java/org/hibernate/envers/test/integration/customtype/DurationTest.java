package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.Duration;

@JiraKey(value = "HHH-17243")
public class DurationTest extends BaseEnversJPAFunctionalTestCase{

	@Entity(name = "Duration")
	@Audited
	public static class DurationTestEntity {
		@Id
		@GeneratedValue 
		private Integer id;

		private Duration duration;

                DurationTestEntity(){
                    
                }
                
		DurationTestEntity(Duration aDuration) {
                    this.duration = aDuration;
		}

		public Integer getId() {
			return id;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration aDuration) {
			this.duration = aDuration;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DurationTestEntity.class };
	}

	private Integer durationId;

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - insert
		this.durationId = doInJPA( this::entityManagerFactory, entityManager -> {
			final DurationTestEntity duration = new DurationTestEntity(Duration.ofHours(2));
			entityManager.persist( duration );
			return duration.getId();
		} );

		// Revision 2 - update
		doInJPA( this::entityManagerFactory, entityManager -> {
			final DurationTestEntity duration = entityManager.find( DurationTestEntity.class, this.durationId );
			duration.setDuration(Duration.ofHours(3));
			entityManager.merge(duration);
		} );
	}    
}
