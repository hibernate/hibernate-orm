package org.hibernate.test.annotations.id.generationmappings;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "DEDICATED_SEQ_TBL1")
public class DedicatedSequenceEntity1 implements Serializable {
	public static final String SEQUENCE_SUFFIX = "_GEN";

	private Long id;

	@Id
	@GeneratedValue(generator = "SequencePerEntityGenerator")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
