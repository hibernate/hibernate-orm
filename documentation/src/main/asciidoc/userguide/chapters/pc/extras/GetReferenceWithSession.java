Book book = new Book();
book.setAuthor( session.byId( Author.class ).getReference( authorId ) );