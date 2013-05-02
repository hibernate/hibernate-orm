package org.hibernate.envers.test.integration.entityNames.oneToManyNotAudited;

import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Hern&aacute;n Chanfreau
 */

public class Car {

	private long id;

	private int number;

	private List<Person> owners;


	public Car() {
	}

	public Car(int number, List<Person> owners) {
		this.number = number;
		this.owners = owners;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public List<Person> getOwners() {
		return owners;
	}

	public void setOwners(List<Person> owners) {
		this.owners = owners;
	}

	@Audited
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}


}
