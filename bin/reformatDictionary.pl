
#!/usr/bin/perl
use strict;
use warnings;

my $filename = $ARGV[0];
my $outputfile = $ARGV[1];
open(my $fh, '<:encoding(UTF-8)', $filename)
  or die "Could not open file '$filename' $!";

while (my $row = <$fh>) {
  chomp $row;
  if($row =~ m/(.*)\t(.*)/) {
    my $headword = $1;
    my $cuis = $2;

    $headword =~ s/ /_/g;
    print "$headword ";

    $cuis =~ s/,/ /g;
    print "$cuis\n";
  }
}
