package org.hibernate.processor.test.ormPanache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PanacheBook extends PanacheEntity {
	public @Id String isbn;
    public @NaturalId String title;
    public @NaturalId String author;
    public String text;
    public int pages;
    
    @Find
    public static native List<PanacheBook> findBook(String isbn);

    @HQL("WHERE isbn = :isbn")
    public static native List<PanacheBook> hqlBook(String isbn);
}
