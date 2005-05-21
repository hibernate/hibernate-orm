#!/bin/env perl

use strict;
use warnings;


open(F, $ARGV[0]) or die "Can't open file $ARGV[0]: $!\n";
  
my $status = 1;

READLINE:
while (<F>) {
  if (/\s+\+{3,}/) { 
    $status = -1; 
    next READLINE; 
  }
  if (/\s+-{3,}/) { 
    $status = 1; 
    next READLINE; 
  }
  if (/\s+={3,}/) { 
    next READLINE; 
  }
  
  if ($status < 0) { 
    next READLINE; 
  }
  
  print $_;
}

