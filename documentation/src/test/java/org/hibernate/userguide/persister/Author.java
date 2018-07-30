/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.persister;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Persister;


/**
 * @author Shawn Clowater
 */
//tag::entity-persister-mapping[]
@Entity
@Persister( impl = EntityPersister.class )
public class Author {

    @Id
    public Integer id;

    @OneToMany( mappedBy = "author" )
    @Persister( impl = CollectionPersister.class )
    public Set<Book> books = new HashSet<>();

    //Getters and setters omitted for brevity

    //end::entity-persister-mapping[]

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Book> getBooks() {
        return books;
    }

    //tag::entity-persister-mapping[]
    public void addBook(Book book) {
        this.books.add( book );
        book.setAuthor( this );
    }
}
//end::entity-persister-mapping[]
