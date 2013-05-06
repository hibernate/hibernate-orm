package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
@Audited
public class AuditedImplementor implements SimpleInterface {
	private long id;
	private String data;
	private String auditedImplementorData;
	private int numerito;

	protected AuditedImplementor() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getAuditedImplementorData() {
		return auditedImplementorData;
	}

	public void setAuditedImplementorData(String implementorData) {
		this.auditedImplementorData = implementorData;
	}

	public int getNumerito() {
		return numerito;
	}

	public void setNumerito(int numerito) {
		this.numerito = numerito;
	}

}
