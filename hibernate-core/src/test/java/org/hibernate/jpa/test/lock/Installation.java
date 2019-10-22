package org.hibernate.jpa.test.lock;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

@Entity
public class Installation {

	@Id
	@Column(name = "installation_uuid")
	private UUID uuid;

	@Version
	@Column(name = "opt_lock", nullable = false)
	private long optLock = 0;

	@ManyToOne(fetch = FetchType.LAZY, optional = false, targetEntity = App.class)
	@JoinColumn(name = "app_uuid", foreignKey = @ForeignKey(name = "installation_app_fk"), nullable = false)
	private App app;

	@Column(name = "details")
	private String details;

	public App getApp() {
		return app;
	}

	public Installation setApp(final App app) {
		this.app = app;
		return this;
	}

	public String getDetails() {
		return details;
	}

	public Installation setDetails(final String details) {
		this.details = details;
		return this;
	}

	public UUID getUuid() {
		return uuid;
	}

	public Installation setUuid(final UUID uuid) {
		this.uuid = uuid;
		return this;
	}

	public long getOptLock() {
		return optLock;
	}


	@Override
	public String toString() {
		return "Installation{" +
				"uuid=" + uuid +
				", optLock=" + optLock +
				", app=" + app +
				", details='" + details + '\'' +
				'}';
	}
}