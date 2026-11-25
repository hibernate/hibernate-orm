package org.hibernate.orm.test.onetoone.embeddedid;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A unidirectional one-to-one association to an entity with an embedded id,
 * where the join column references a column which is not part of that id.
 */
@DomainModel(annotatedClasses = { OneToOneUniqueKeyEmbeddedIdTest.Port.class,
		OneToOneUniqueKeyEmbeddedIdTest.Device.class })
@SessionFactory
@JiraKey("HHH-16961")
class OneToOneUniqueKeyEmbeddedIdTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Port port = new Port();
			port.coordinate = new Coordinate( 1, 2 );
			port.connectionId = 3;
			session.persist( port );
			final Device device = new Device();
			device.id = 4;
			device.port = port;
			session.persist( device );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void foreignKeyHoldsReferencedColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select connection_id from uk_device", Integer.class ).getSingleResult() ).isEqualTo( 3 ) );
	}

	@Test
	void find(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertPort( session.find( Device.class, 4 ) ) );
	}

	@Test
	void query(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertPort(
				session.createSelectionQuery( "from Device", Device.class ).getSingleResult() ) );
	}

	private static void assertPort(Device device) {
		assertThat( device.port ).isNotNull();
		assertThat( device.port.connectionId ).isEqualTo( 3 );
		assertThat( device.port.coordinate ).isEqualTo( new Coordinate( 1, 2 ) );
	}

	@Embeddable
	static class Coordinate implements Serializable {
		@Column(name = "slot_number")
		Integer slotNumber;
		@Column(name = "port_number")
		Integer portNumber;

		Coordinate() {
		}

		Coordinate(Integer slotNumber, Integer portNumber) {
			this.slotNumber = slotNumber;
			this.portNumber = portNumber;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof Coordinate that
				&& Objects.equals( slotNumber, that.slotNumber )
				&& Objects.equals( portNumber, that.portNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hash( slotNumber, portNumber );
		}
	}

	@Entity(name = "Port")
	@Table(name = "uk_port")
	static class Port implements Serializable {
		@EmbeddedId
		Coordinate coordinate;
		@Column(name = "connection_id")
		Integer connectionId;
	}

	@Entity(name = "Device")
	@Table(name = "uk_device")
	static class Device {
		@Id
		Integer id;
		@OneToOne
		@JoinColumn(name = "connection_id", referencedColumnName = "connection_id")
		Port port;
	}
}
