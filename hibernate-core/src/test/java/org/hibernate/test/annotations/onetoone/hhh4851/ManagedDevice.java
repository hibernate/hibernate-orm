package org.hibernate.test.annotations.onetoone.hhh4851;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * this class represents a logical representation of a terminal it could be linked to a terminal or not it contains the
 * alias of the terminal and is virtualizable
 */
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class ManagedDevice extends BaseEntity {

	private String name;
	private Device device;
	private DeviceGroupConfig deviceGroupConfig = null;

	public ManagedDevice() {
	}

	public ManagedDevice(String alias) {
		this.name = alias;
	}

	public String getName() {
		return name;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "terminal_id")
	public Device getDevice() {
		return device;
	}

	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	@JoinTable(name = "ManDev_DevGroupConf",
			joinColumns = { @JoinColumn(name = "MavDev_id", unique = true) },
			inverseJoinColumns = { @JoinColumn(name = "DevGroupConf_id") })
	public DeviceGroupConfig getDeviceGroupConfig() {
		return deviceGroupConfig;
	}

	public void setName(String alias) {
		this.name = alias;
	}

	public void setDevice(Device terminal) {
		this.device = terminal;
	}

	public void setDeviceGroupConfig(DeviceGroupConfig terminalGroup) {
		this.deviceGroupConfig = terminalGroup;
	}

}
