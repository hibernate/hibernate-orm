//$Id$
package org.hibernate.test.annotations.query;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorColumn(name = "disc")
@DiscriminatorValue("Dic")
@SqlResultSetMapping(
		name = "dictionary", entities = {
@EntityResult(
		entityClass = org.hibernate.test.annotations.query.Dictionary.class,
		fields = {
		@FieldResult(name = "id", column = "id"),
		@FieldResult(name = "name", column = "name"),
		@FieldResult(name = "editor", column = "editor")
				},
		discriminatorColumn = "`type`"
)
		}
)
@NamedNativeQuery(name = "all.dictionaries",
		query = "select id, name, editor, disc as type from Dictionary",
		resultSetMapping = "dictionary")
public class Dictionary {
	private Integer id;
	private String name;
	private String editor;

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

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}
}
