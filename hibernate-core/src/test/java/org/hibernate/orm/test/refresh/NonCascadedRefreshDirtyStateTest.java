package org.hibernate.orm.test.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Adapted from the HHH-21044 reproducer by Pål Simen Ellingsen.
 * {@code refresh(connectivity)} must not touch {@link DeviceStatus}: the path from {@link DeviceConnectivity} goes
 * through {@code DeviceConnectivity.device}, which has no cascade.
 * <p>
 * Hibernate 7.2.25.Final: passes. Hibernate 7.4.13.Final: the managed {@link DeviceStatus} is reloaded from the
 * database, so its pending (unflushed) change is silently discarded and never written.
 */
@DomainModel(annotatedClasses = {
		NonCascadedRefreshDirtyStateTest.Pole.class,
		NonCascadedRefreshDirtyStateTest.Device.class,
		NonCascadedRefreshDirtyStateTest.DeviceStatus.class,
		NonCascadedRefreshDirtyStateTest.DeviceConnectivity.class
})
@SessionFactory
@JiraKey("HHH-21044")
public class NonCascadedRefreshDirtyStateTest {

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Pole pole = new Pole(1);
			session.persist(pole);
			Device device = new Device(pole);
			device.status = new DeviceStatus(device);
			session.persist(device); // cascades to DeviceStatus and DeviceConnectivity
			pole.device = device;
		});
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void refreshDoesNotDiscardPendingChangeOfNonCascadedEntity(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Pole pole = session.find(Pole.class, 1);
			DeviceStatus status = pole.device.status;
			DeviceConnectivity connectivity = status.connectivity;
			Hibernate.initialize(connectivity);

			status.dimPercent = 100; // pending change, not flushed yet

			// E.g. after updating the connectivity row with native SQL
			session.refresh(connectivity);

			// 7.4.13.Final: expected 100 but was null
			assertThat(pole.device.status).isSameAs(status);
			assertThat(status.dimPercent).isEqualTo(100);
		});

		// The change must also reach the database at commit
		scope.inTransaction(session -> assertThat(session.find(DeviceStatus.class, 1).dimPercent).isEqualTo(100));
	}

	@Entity(name = "Pole")
	static class Pole {
		@Id
		Integer id;

		@OneToOne
		@JoinColumn(name = "device_id")
		Device device;

		Pole() {
		}

		Pole(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Device")
	static class Device {
		@Id
		Integer id;

		@OneToOne(optional = false)
		@MapsId
		@PrimaryKeyJoinColumn
		Pole pole;

		@OneToOne(mappedBy = "device", cascade = CascadeType.ALL)
		DeviceStatus status;

		Device() {
		}

		Device(Pole pole) {
			this.pole = pole;
		}
	}

	@Entity(name = "DeviceStatus")
	static class DeviceStatus {
		@Id
		Integer id;

		@OneToOne
		@MapsId
		@PrimaryKeyJoinColumn
		Device device;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
		@PrimaryKeyJoinColumn
		DeviceConnectivity connectivity;

		Integer dimPercent;

		DeviceStatus() {
		}

		DeviceStatus(Device device) {
			this.device = device;
			this.connectivity = new DeviceConnectivity(device);
		}
	}

	@Entity(name = "DeviceConnectivity")
	static class DeviceConnectivity {
		@Id
		Integer id;

		@OneToOne // no cascade
		@MapsId
		@PrimaryKeyJoinColumn
		Device device;

		String onlineStatus;

		DeviceConnectivity() {
		}

		DeviceConnectivity(Device device) {
			this.device = device;
		}
	}
}
