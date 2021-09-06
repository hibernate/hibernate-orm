/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.beanvalidation;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 * @author Geonhee Lee
 */
@Entity
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Column(name = "title")
    private String title;

    @NotEmpty
    @Column(name = "author")
    private String author;

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
