Book book = new Book();
book.setAuthor( entityManager.getReference( Author.class, authorId ) );