
#!/usr/bin/perl
use strict;
use warnings;

my $filename = $ARGV[0];
my $outputfile = $ARGV[1];
open(my $fh, '<:encoding(UTF-8)', $filename)
  or die "Could not open file '$filename' $!";

while (my $row = <$fh>) {
  chomp $row;
  if($row =~ m/(.*)\t(.*)\t(.*)/) {
    my $source = $1;
    my $target = $2;
    my $weight = $3;
    print "u:$source v:$target w:$weight\n";
  }
}
