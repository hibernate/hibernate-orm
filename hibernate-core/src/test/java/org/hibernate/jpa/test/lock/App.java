package org.hibernate.jpa.test.lock;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class App {

	@Id
	private UUID uuid;

	@Version
	@Column(name = "opt_lock", nullable = false)
	private long optLock = 0;

	@Column(name = "name")
	private String name;


	public UUID getUuid() {
		return uuid;
	}

	public App setUuid(final UUID uuid) {
		this.uuid = uuid;
		return this;
	}

	public String getName() {
		return name;
	}

	public App setName(final String name) {
		this.name = name;
		return this;
	}

	public long getOptLock() {
		return optLock;
	}

	@Override
	public String toString() {
		return "App{" +
				"uuid=" + uuid +
				", optLock=" + optLock +
				", name='" + name + '\'' +
				'}';
	}
}
