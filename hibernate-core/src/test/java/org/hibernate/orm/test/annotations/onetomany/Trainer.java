/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

/**
 * Unidirectional one to many sample
 *
 * @author Emmanuel Bernard
 */
@Entity()
public class Trainer {
	private Integer id;
	private String name;
	private Set<Tiger> trainedTigers;
	private Set<Monkey> trainedMonkeys;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany
	public Set<Tiger> getTrainedTigers() {
		return trainedTigers;
	}

	public void setTrainedTigers(Set<Tiger> trainedTigers) {
		this.trainedTigers = trainedTigers;
	}

	@OneToMany
	@JoinTable(
			name = "TrainedMonkeys",
			//columns are optional, here we explicit them
			joinColumns = @JoinColumn(name = "trainer_id"),
			inverseJoinColumns = @JoinColumn(name = "monkey_id")
	)
//	@ForeignKey(name = "TM_TRA_FK", inverseName = "TM_MON_FK")
	public Set<Monkey> getTrainedMonkeys() {
		return trainedMonkeys;
	}

	public void setTrainedMonkeys(Set<Monkey> trainedMonkeys) {
		this.trainedMonkeys = trainedMonkeys;
	}
}
