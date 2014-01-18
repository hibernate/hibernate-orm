package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * @author Oleksander Dukhno
 */
@Entity
public class Question {

	@Id
	private long id;
	@OneToMany(
			mappedBy = "question"
	)
	private List<StudentAnswer> answers;

	public Question() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<StudentAnswer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<StudentAnswer> answers) {
		this.answers = answers;
	}
}
